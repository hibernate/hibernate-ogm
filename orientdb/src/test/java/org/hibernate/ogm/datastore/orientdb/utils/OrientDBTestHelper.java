/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.orientdb.OrientDB;
import org.hibernate.ogm.datastore.orientdb.OrientDBDialect;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.GridDialectTestHelper;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBTestHelper implements GridDialectTestHelper {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public long getNumberOfEntities(Session session) {
		return getNumberOfEntities( session.getSessionFactory() );
	}

	private boolean isSystemClass(OClass schemaClass) {
		boolean result = ( schemaClass.getName().charAt( 0 ) == 'O' && Character.isUpperCase( schemaClass.getName().charAt( 1 ) ) ) ||
				( schemaClass.getName().equals( "E" ) ) ||
				( schemaClass.getName().equals( "V" ) ) ||
				( schemaClass.getName().equals( "sequences" ) );
		return result;
	}

	private boolean isAssociationClass(OClass schemaClass) {
		Map<String, OProperty> properties = schemaClass.propertiesMap();
		log.debugf( "properties %s for class %s ", properties.keySet(), schemaClass.getName() );
		int count = properties.size();
		boolean result = false;
		if ( count == 2 ) {
			boolean allFieldsEndsWith_id = false;
			for ( String fieldName : properties.keySet() ) {
				if ( fieldName.endsWith( "_id" ) ) {
					allFieldsEndsWith_id = true;
				}
				else {
					allFieldsEndsWith_id = false;
					break;
				}
			}
			result = allFieldsEndsWith_id;
		}
		return result;
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		OrientDBDatastoreProvider provider = getProvider( sessionFactory );
		ODatabaseDocumentTx db = provider.getCurrentDatabase();
		long result = 0;
		OSchema schema = db.getMetadata().getSchema();
		for ( OClass schemaClass : schema.getClasses() ) {
			if ( !isSystemClass( schemaClass ) && !isAssociationClass( schemaClass ) ) {
				List<ODocument> docs = NativeQueryUtil.executeIdempotentQuery( db, "select from " + schemaClass.getName() );
				log.debugf( "found %d entities in class %s ", docs.size(), schemaClass.getName() );
				result += docs.size();
			}
		}
		return result;
	}

	@Override
	public long getNumberOfAssociations(Session session) {
		SessionFactory sessionFactory = session.getSessionFactory();
		return getNumberOfAssociations( sessionFactory );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		OrientDBDatastoreProvider provider = getProvider( sessionFactory );
		ODatabaseDocumentTx db = provider.getCurrentDatabase();
		long result = 0;
		OSchema schema = db.getMetadata().getSchema();
		for ( OClass schemaClass : schema.getClasses() ) {
			if ( !isSystemClass( schemaClass ) && isAssociationClass( schemaClass ) ) {
				List<ODocument> docs = NativeQueryUtil.executeIdempotentQuery( db, "select from " + schemaClass.getName() );
				log.debugf( "found %d associations in class %s ", docs.size(), schemaClass.getName() );
				result += docs.size();
			}
		}
		return result;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		Map<String, Object> tuple = new HashMap<>();
		GridDialect dialect = getDialect( session.getSessionFactory() );
		TupleSnapshot snapshot = dialect.getTuple( key, GridDialectOperationContexts.emptyTupleContext() ).getSnapshot();
		for ( String column : snapshot.getColumnNames() ) {
			tuple.put( column, snapshot.get( column ) );
		}
		return tuple;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void prepareDatabase(SessionFactory sessionFactory) {
		log.info( "--- preparing database ----" );
		OrientDBDatastoreProvider provider = getProvider( sessionFactory );
		ConfigurationPropertyReader propertyReader = provider.getPropertyReader();
		ODatabaseDocumentTx db = provider.getCurrentDatabase();
		log.infof( "call prepareDatabase! db closed: %s ", db.isClosed() );
		NativeQueryUtil.executeNonIdempotentQuery( db, "ALTER DATABASE TIMEZONE UTC" );
		NativeQueryUtil.executeNonIdempotentQuery( db, "ALTER DATABASE DATEFORMAT '"
				.concat( PropertyReaderUtil.readDateFormatProperty( propertyReader ) )
				.concat( "'" ) );
		NativeQueryUtil.executeNonIdempotentQuery( db, "ALTER DATABASE DATETIMEFORMAT '"
				.concat( PropertyReaderUtil.readDateTimeFormatProperty( propertyReader ) )
				.concat( "'" ) );

	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		log.infof( "call dropSchemaAndDatabase!" );
		OrientDBDatastoreProvider provider = getProvider( sessionFactory );
		ODatabaseDocumentTx db = provider.getCurrentDatabase();
		db.drop();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return readProperties();
	}

	private static Map<String, String> readProperties() {
		try {
			Properties hibProperties = new Properties();
			hibProperties.load( Thread.currentThread().getContextClassLoader().getResourceAsStream( "hibernate.properties" ) );
			Map<String, String> props = new HashMap<>();
			for ( Map.Entry<Object, Object> entry : hibProperties.entrySet() ) {
				props.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
				log.info( entry.toString() );
			}
			return Collections.unmodifiableMap( props );
		}
		catch (IOException e) {
			throw new RuntimeException( "Missing properties file: hibernate.properties" );
		}
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return OrientDB.class;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new OrientDBDialect( (OrientDBDatastoreProvider) datastoreProvider );
	}

	private static OrientDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( OrientDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with OrientDB, cannot extract underlying provider" );
		}
		return OrientDBDatastoreProvider.class.cast( provider );
	}

	private static GridDialect getDialect(SessionFactory sessionFactory) {
		GridDialect dialect = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( GridDialect.class );
		return dialect;
	}

}
