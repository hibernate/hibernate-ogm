/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test which makes sure that {@link InfinispanDialect} and {@link InfinispanDatastoreProvider} can operate
 * in clustered mode, in particular that objects can be serialized and de-serialized when being written into and read
 * from the data grid.
 * <p>
 *
 * @author Gunnar Morling
 */
public class InfinispanDialectWithClusteredConfigurationTest {

	private static InfinispanDatastoreProvider provider1;
	private static InfinispanDatastoreProvider provider2;
	private static InfinispanDialect dialect1;
	private static InfinispanDialect dialect2;

	@BeforeClass
	public static void setupProvidersAndDialects() throws Exception {
		SessionFactoryImplementor sessionFactory1 = getSessionFactory();
		SessionFactoryImplementor sessionFactory2 = getSessionFactory();
		provider1 = (InfinispanDatastoreProvider) sessionFactory1.getServiceRegistry().getService( DatastoreProvider.class );
		provider2 = (InfinispanDatastoreProvider) sessionFactory2.getServiceRegistry().getService( DatastoreProvider.class );
		dialect1 = new InfinispanDialect( provider1 );
		dialect2 = new InfinispanDialect( provider2 );

		provider1.getSchemaDefinerType().newInstance().initializeSchema( null, sessionFactory1 );
		provider2.getSchemaDefinerType().newInstance().initializeSchema( null, sessionFactory2 );
	}

	@AfterClass
	public static void stopProvider() {
		provider1.stop();
		provider2.stop();
	}

	@Test
	public void shouldWriteAndReadTupleInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		Tuple tuple = dialect1.createTuple( key, emptyTupleContext() );
		tuple.put( "foo", "bar" );
		dialect1.insertOrUpdateTuple( key, tuple, emptyTupleContext() );

		// then
		Tuple readTuple = dialect2.getTuple( key, null );
		assertThat( readTuple.get( "foo" ) ).isEqualTo( "bar" );
	}

	@Test
	public void shoulReadAndWriteSequenceInClusteredMode() throws Exception {
		// given
		IdSourceKeyMetadata keyMetadata = DefaultIdSourceKeyMetadata.forTable( "Hibernate_Sequences", "sequence_name", "next_val" );
		IdSourceKey key = IdSourceKey.forTable( keyMetadata, "Foo_Sequence" );

		// when
		Number value = dialect1.nextValue( new NextValueRequest( key, 1, 1 ) );
		assertThat( value ).isEqualTo( 1L );

		// then
		value = dialect2.nextValue( new NextValueRequest( key, 1, 1 ) );
		assertThat( value ).isEqualTo( 2L );
	}

	@Test
	public void shouldWriteAndReadAssociationInClusteredMode() throws Exception {
		// given
		String[] columnNames = { "foo", "bar", "baz" };
		AssociationKeyMetadata keyMetadata = new DefaultAssociationKeyMetadata.Builder()
				.table( "Foobar" )
				.columnNames( columnNames )
				.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( null, null ) )
				.build();
		Object[] values = { 123, "Hello", 456L };

		AssociationKey key = new AssociationKey( keyMetadata, values, null );

		RowKey rowKey = new RowKey( columnNames, values );
		Tuple tuple = new Tuple();
		tuple.put( "zip", "zap" );

		// when
		Association association = dialect1.createAssociation( key, null );
		association.put( rowKey, tuple );
		dialect1.insertOrUpdateAssociation( key, association, null );

		// then
		Association readAssociation = dialect2.getAssociation( key, null );
		Tuple readKey = readAssociation.get( rowKey );
		assertThat( readKey ).isNotNull();
		assertThat( readKey.get( "zip" ) ).isEqualTo( "zap" );
	}


	private static InfinispanDatastoreProvider createAndStartNewProvider(ServiceRegistryImplementor serviceRegistry) {
		Map<String, Object> configurationValues = new HashMap<String, Object>();
		configurationValues.put( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-dist-duplicate-domains-allowed.xml" );
		InfinispanDatastoreProvider provider = new InfinispanDatastoreProvider();

		provider.configure( configurationValues );
		provider.injectServices( serviceRegistry );
		provider.start();

		return provider;
	}

	private static ServiceRegistryImplementor getServiceRegistry() {
		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );

		JBossStandAloneJtaPlatform jtaPlatform = new JBossStandAloneJtaPlatform();
		jtaPlatform.injectServices( serviceRegistry );
		when( serviceRegistry.getService( JtaPlatform.class ) ).thenReturn( jtaPlatform );

		InfinispanDatastoreProvider provider = createAndStartNewProvider( serviceRegistry );
		when( serviceRegistry.getService( DatastoreProvider.class ) ).thenReturn( provider );

		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( new ClassLoaderServiceImpl() );

		return serviceRegistry;
	}

	private static SessionFactoryImplementor getSessionFactory() {
		SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );

		OgmEntityPersister foobarPersister = mock( OgmEntityPersister.class );
		when( foobarPersister.getEntityKeyMetadata() ).thenReturn( new DefaultEntityKeyMetadata( "Foobar", new String[] {} ) );
		when( foobarPersister.getPropertyNames() ).thenReturn( new String[] {} );
		when( sessionFactory.getEntityPersisters() ).thenReturn( Collections.<String, EntityPersister> singletonMap( "Foobar", foobarPersister ) );

		OgmCollectionPersister foobarCollectionPersister = mock( OgmCollectionPersister.class );
		when( foobarCollectionPersister.getAssociationKeyMetadata() ).thenReturn( new DefaultAssociationKeyMetadata.Builder().table( "Foobar" ).build() );
		when( sessionFactory.getCollectionPersisters() ).thenReturn(
				Collections.<String, CollectionPersister> singletonMap( "Foobar", foobarCollectionPersister ) );

		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();
		when( sessionFactory.getServiceRegistry() ).thenReturn( serviceRegistry );

		return sessionFactory;
	}
}
