/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.utils;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.utils.TestableGridDialect;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class MongoDBTestHelper implements TestableGridDialect {

	private static final Log log = LoggerFactory.getLogger();

	static {
		// Read host and port from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String mongoHostName = System.getenv( "MONGODB_HOSTNAME" );
		if ( isNotNull( mongoHostName ) ) {
			System.getProperties().setProperty( OgmProperties.HOST, mongoHostName );
		}
		String mongoPort = System.getenv( "MONGODB_PORT" );
		if ( isNotNull( mongoPort ) ) {
			System.getProperties().setProperty( OgmProperties.PORT, mongoPort );
		}
	}

	private static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DB db = provider.getDatabase();
		int count = 0;

		for ( String collectionName : getEntityCollections( sessionFactory ) ) {
			count += db.getCollection( collectionName ).count();
		}

		return count;
	}

	private boolean isSystemCollection(String collectionName) {
		return collectionName.startsWith( "system." );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		long associationCount = getNumberOfAssociationsFromGlobalCollection( sessionFactory );
		associationCount += getNumberOfAssociationsFromDedicatedCollections( sessionFactory );
		associationCount += getNumberOfEmbeddedAssociations( sessionFactory );

		return associationCount;
	}

	public long getNumberOfAssociationsFromGlobalCollection(SessionFactory sessionFactory) {
		DB db = getProvider( sessionFactory ).getDatabase();
		return db.getCollection( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE ).count();
	}

	public long getNumberOfAssociationsFromDedicatedCollections(SessionFactory sessionFactory) {
		DB db = getProvider( sessionFactory ).getDatabase();

		Set<String> associationCollections = getDedicatedAssociationCollections( sessionFactory );
		long associationCount = 0;
		for ( String collectionName : associationCollections ) {
			associationCount += db.getCollection( collectionName ).count();
		}

		return associationCount;
	}

	// TODO Use aggregation framework for a more efficient solution; Given that there will only be a few
	// test collections/entities, that's good enough for now
	public long getNumberOfEmbeddedAssociations(SessionFactory sessionFactory) {
		DB db = getProvider( sessionFactory ).getDatabase();
		long associationCount = 0;

		for ( String entityCollection : getEntityCollections( sessionFactory ) ) {
			DBCursor entities = db.getCollection( entityCollection ).find();

			while ( entities.hasNext() ) {
				DBObject entity = entities.next();
				associationCount += getNumberOfEmbeddedAssociations( entity );
			}
		}

		return associationCount;
	}

	private int getNumberOfEmbeddedAssociations(DBObject entity) {
		int numberOfReferences = 0;

		for ( String fieldName : entity.keySet() ) {
			Object field = entity.get( fieldName );
			if ( isAssociation( field ) ) {
				numberOfReferences++;
			}
		}

		return numberOfReferences;
	}

	private boolean isAssociation(Object field) {
		return ( field instanceof List );
	}

	private Set<String> getEntityCollections(SessionFactory sessionFactory) {
		DB db = MongoDBTestHelper.getProvider( sessionFactory ).getDatabase();
		Set<String> names = new HashSet<String>();

		for ( String collectionName : db.getCollectionNames() ) {
			if ( !isSystemCollection( collectionName ) &&
					!isDedicatedAssociationCollection( collectionName ) &&
					!isGlobalAssociationCollection( collectionName ) ) {
				names.add( collectionName );
			}
		}

		return names;
	}

	private Set<String> getDedicatedAssociationCollections(SessionFactory sessionFactory) {
		DB db = MongoDBTestHelper.getProvider( sessionFactory ).getDatabase();
		Set<String> names = new HashSet<String>();

		for ( String collectionName : db.getCollectionNames() ) {
			if ( isDedicatedAssociationCollection( collectionName ) ) {
				names.add( collectionName );
			}
		}

		return names;
	}

	private boolean isDedicatedAssociationCollection(String collectionName) {
		return collectionName.startsWith( MongoDBDialect.ASSOCIATIONS_COLLECTION_PREFIX );
	}

	private boolean isGlobalAssociationCollection(String collectionName) {
		return collectionName.equals( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DBObject finder = new BasicDBObject( MongoDBDialect.ID_FIELDNAME, key.getColumnValues()[0] );
		DBObject result = provider.getDatabase().getCollection( key.getTable() ).findOne( finder );
		replaceIdentifierColumnName( result, key );
		return result.toMap();
	}

	/**
	 * The MongoDB dialect replaces the name of the column identifier, so when the tuple is extracted from the db
	 * we replace the column name of the identifier with the original one.
	 * We are assuming the identifier is not embedded and is a single property.
	 */
	private void replaceIdentifierColumnName(DBObject result, EntityKey key) {
		Object idValue = result.get( MongoDBDialect.ID_FIELDNAME );
		result.removeField( MongoDBDialect.ID_FIELDNAME );
		result.put( key.getColumnNames()[0], idValue );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	private static MongoDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( MongoDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with MongoDB, cannot extract underlying cache" );
		}
		return MongoDBDatastoreProvider.class.cast( provider );
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = getProvider( sessionFactory );
		try {
			provider.getDatabase().dropDatabase();
		}
		catch ( MongoException ex ) {
			throw log.unableToDropDatabase( ex, provider.getDatabase().getName() );
		}
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		//read variables from the System properties set in the static initializer
		Map<String,String> envProps = new HashMap<String, String>(2);
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PORT, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PASSWORD, envProps );
		return envProps;
	}

	private void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		switch ( type ) {
			case ASSOCIATION_DOCUMENT:
				return getNumberOfAssociationsFromGlobalCollection( sessionFactory );
			case IN_ENTITY:
				return getNumberOfEmbeddedAssociations( sessionFactory );
			default:
				throw new IllegalArgumentException( "Unexpected association storaget type " + type );
		}
	}

	@Override
	public MongoDBGlobalContext configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( MongoDB.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new MongoDBDialect( (MongoDBDatastoreProvider) datastoreProvider );
	}

	public static void assertDbObject(OgmSessionFactory sessionFactory, String collection, String queryDbObject, String expectedDbObject) {
		DBObject finder = (DBObject) JSON.parse( queryDbObject );
		DBObject expected = (DBObject) JSON.parse( expectedDbObject );

		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DBObject actual = provider.getDatabase().getCollection( collection ).findOne( finder );

		assertThat( actual ).isEqualTo( expected );
	}
}
