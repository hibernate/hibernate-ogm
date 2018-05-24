/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDataStoreConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtoStreamMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ExceptionalProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.ResultSetProcedure;
import org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures.SimpleValueProcedure;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.persister.impl.SingleTableOgmEntityPersister;
import org.hibernate.ogm.utils.BaseGridDialectTestHelper;
import org.hibernate.ogm.utils.GridDialectTestHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.query.dsl.Query;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public class InfinispanRemoteTestHelper extends BaseGridDialectTestHelper implements GridDialectTestHelper {

	private static final String SERVER_TASK_META_INF_RESOURCE_DIRECTORY = "/storedprocedures/servertask";

	private static final String SERVER_TASK_META_INF_TARGET_FILE = "/META-INF/services/org.infinispan.tasks.ServerTask";
	private static final String INFINISPAN_DEPLOYMENTS_DIRECTORY = "target/infinispan-server/standalone/deployments";

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		final InfinispanRemoteDatastoreProvider datastoreProvider = getProvider( sessionFactory );
		final SessionFactoryImplementor sessionFactoryImplementor = getSessionFactoryImplementor( sessionFactory );
		Collection<CollectionPersister> persisters = sessionFactoryImplementor.getMetamodel().collectionPersisters().values();
		long totalCount = 0;
		for ( CollectionPersister ep : persisters ) {
			OgmCollectionPersister persister = (OgmCollectionPersister) ep;
			String tableName = persister.getTableName();
			SingleTableOgmEntityPersister owningPersister = (SingleTableOgmEntityPersister) persister.getOwnerEntityPersister().getEntityPersister();
			String ownerTableName = owningPersister.getTableName();
			totalCount += countAssociations( tableName, ownerTableName, datastoreProvider );
		}
		return totalCount;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		final InfinispanRemoteDatastoreProvider datastoreProvider = getProvider( sessionFactory );
		final Set<String> mappedCacheNames = datastoreProvider.getMappedCacheNames();
		mappedCacheNames.forEach( cacheName -> datastoreProvider.getCache( cacheName ).clear() );
	}

	@Override
	public Map<String, String> getAdditionalConfigurationProperties() {
		return Collections.singletonMap( OgmProperties.CREATE_DATABASE, "true" );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		if ( type == AssociationStorageType.IN_ENTITY ) {
			throw new IllegalArgumentException( "IN_ENTITY association storage type not supported" );
		}
		else {
			return getNumberOfAssociations( sessionFactory );
		}
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new InfinispanRemoteDialect( (InfinispanRemoteDatastoreProvider) datastoreProvider );
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return InfinispanRemoteDataStoreConfiguration.class;
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		final InfinispanRemoteDatastoreProvider datastoreProvider = getProvider( session );
		ProtoStreamMappingAdapter mapper = datastoreProvider.getDataMapperForCache( key.getTable() );
		ProtostreamId idBuffer = mapper.createIdPayload( key.getColumnNames(), key.getColumnValues() );
		ProtostreamPayload payload = mapper.withinCacheEncodingContext( c -> c.get( idBuffer ) );
		return payload.toMap();
	}

	private long countAssociations(String tableName, String ownerTableName, InfinispanRemoteDatastoreProvider datastoreProvider) {
		final String[] ownerIdentifyingColumnNames = datastoreProvider.getDataMapperForCache( ownerTableName ).listIdColumnNames();
		final ProtoStreamMappingAdapter mapper = datastoreProvider.getDataMapperForCache( tableName );
		return mapper.withinCacheEncodingContext( c -> {
			Query queryAll = Search.getQueryFactory( c ).from( ProtostreamPayload.class ).build();
			Set<RowKey> resultsCollector = new HashSet<>();
			try ( CloseableIterator<Entry<Object,Object>> iterator = c.retrieveEntriesByQuery( queryAll, null, 100 ) ) {
				while ( iterator.hasNext() ) {
					Entry<Object,Object> e  = iterator.next();
					ProtostreamPayload value = ( (ProtostreamPayload) e.getValue() );
					Map<String, Object> entryObject = value.toMap();
					Object[] columnValues = new Object[ownerIdentifyingColumnNames.length];
					for ( int i = 0; i < columnValues.length; i++ ) {
						columnValues[i] = entryObject.get( ownerIdentifyingColumnNames[i] );
					}
					RowKey entryKey = new RowKey( ownerIdentifyingColumnNames, columnValues );
					resultsCollector.add( entryKey );
				}
			}
			return (long) resultsCollector.size();
		} );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		final InfinispanRemoteDatastoreProvider datastoreProvider = getProvider( sessionFactory );
		final SessionFactoryImplementor sessionFactoryImplementor = getSessionFactoryImplementor( sessionFactory );
		Collection<EntityPersister> persisters = sessionFactoryImplementor.getMetamodel().entityPersisters().values();
		final AtomicLong counter = new AtomicLong();
		for ( EntityPersister ep : persisters ) {
			OgmEntityPersister persister = (OgmEntityPersister) ep;
			String tableName = persister.getTableName();
			int increment = datastoreProvider.getCache( tableName ).size();
			counter.addAndGet( increment );
		}
		return counter.get();
	}

	// Various static helpers below:

	public static void registerScriptStoredProcedures(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = getProvider( sessionFactory );
		RemoteCache<String, String> scriptCache = provider.getScriptCache();
		scriptCache.put( Car.SIMPLE_VALUE_PROC, toResourceString( "/storedprocedures/simpleValueProcedure.js" ) );
		scriptCache.put( Car.RESULT_SET_PROC, toResourceString( "/storedprocedures/resultSetProcedure.js" ) );
		scriptCache.put( "exceptionalProcedure", toResourceString( "/storedprocedures/exceptionalProcedure.js" ) );
	}

	public static void clearScriptStoredProcedures(SessionFactory sessionFactory) {
		InfinispanRemoteDatastoreProvider provider = getProvider( sessionFactory );
		RemoteCache<Object, Object> scriptCache = provider.getScriptCache();
		scriptCache.remove( Car.SIMPLE_VALUE_PROC );
		scriptCache.remove( Car.RESULT_SET_PROC );
		scriptCache.remove( "exceptionalProcedure" );
	}

	public static void deployJavaStoredProcedures() {
		ShrinkWrap.create( JavaArchive.class, "simple-value-procedure.jar" )
				.addClass( SimpleValueProcedure.class )
				.addAsResource( asResource( SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/simple-value-procedure" ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( INFINISPAN_DEPLOYMENTS_DIRECTORY + "/simple-value-procedure.jar" ), true );
		ShrinkWrap.create( JavaArchive.class, "result-set-procedure.jar" )
				.addClass( Car.class )
				.addClass( ResultSetProcedure.class )
				.addAsResource( asResource( SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/result-set-procedure" ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( INFINISPAN_DEPLOYMENTS_DIRECTORY + "/result-set-procedure.jar" ), true );
		ShrinkWrap.create( JavaArchive.class, INFINISPAN_DEPLOYMENTS_DIRECTORY + "exceptional-procedure.jar" )
				.addClass( ExceptionalProcedure.class )
				.addAsResource( asResource( SERVER_TASK_META_INF_RESOURCE_DIRECTORY + "/exceptional-procedure" ), SERVER_TASK_META_INF_TARGET_FILE )
				.as( ZipExporter.class )
				.exportTo( new File( INFINISPAN_DEPLOYMENTS_DIRECTORY + "/exceptional-procedure.jar" ), true );
	}

	private static SessionFactoryImplementor getSessionFactoryImplementor(SessionFactory sessionFactory) {
		return ( (SessionFactoryImplementor) sessionFactory );
	}

	public static InfinispanRemoteDatastoreProvider getProvider(Session session) {
		return getProvider( session.getSessionFactory() );
	}

	public static InfinispanRemoteDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( InfinispanRemoteDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Infinispan Remote, cannot extract underlying cache" );
		}
		return InfinispanRemoteDatastoreProvider.class.cast( provider );
	}

	private static URL asResource(String resource) {
		return InfinispanRemoteTestHelper.class.getResource( resource );
	}

	private static String toResourceString(String path) {
		return toString( InfinispanRemoteTestHelper.class.getResourceAsStream( path ) );
	}

	private static String toString(InputStream is) {
		try ( Scanner scanner = new Scanner( is ) ) {
			return scanner.useDelimiter( "\\A" ).next();
		}
	}
}
