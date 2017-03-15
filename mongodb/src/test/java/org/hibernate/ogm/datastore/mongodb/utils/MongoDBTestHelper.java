/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.MongoCursor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.exception.impl.Exceptions;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.utils.GridDialectTestHelper;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import com.mongodb.MongoException;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
public class MongoDBTestHelper implements GridDialectTestHelper {

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
	public long getNumberOfEntities(Session session) {
		return getNumberOfEntities( session.getSessionFactory() );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		MongoDatabase db = provider.getDatabase();
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
	public long getNumberOfAssociations(Session session) {
		return getNumberOfAssociations( session.getSessionFactory() );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		long associationCount = getNumberOfAssociationsFromGlobalCollection( sessionFactory );
		associationCount += getNumberOfAssociationsFromDedicatedCollections( sessionFactory );
		associationCount += getNumberOfEmbeddedAssociations( sessionFactory );

		return associationCount;
	}

	public long getNumberOfAssociationsFromGlobalCollection(SessionFactory sessionFactory) {
		MongoDatabase db = getProvider( sessionFactory ).getDatabase();
		return db.getCollection( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE ).count();
	}

	public long getNumberOfAssociationsFromDedicatedCollections(SessionFactory sessionFactory) {
		MongoDatabase db = getProvider( sessionFactory ).getDatabase();

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
		MongoDatabase db = getProvider( sessionFactory ).getDatabase();
		long associationCount = 0;

		for ( String entityCollection : getEntityCollections( sessionFactory ) ) {
			MongoCursor<Document> entities = db.getCollection( entityCollection ).find().iterator();

			while ( entities.hasNext() ) {
				Document entity = entities.next();
				associationCount += getNumberOfEmbeddedAssociations( entity );
			}
		}

		return associationCount;
	}

	private int getNumberOfEmbeddedAssociations(Document entity) {
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
		MongoDatabase db = MongoDBTestHelper.getProvider( sessionFactory ).getDatabase();
		Set<String> names = new HashSet<>();
		MongoCursor<String> collections = db.listCollectionNames().iterator();

		while ( collections.hasNext() ) {
			String collectionName = collections.next();
			if ( !isSystemCollection( collectionName ) &&
					!isDedicatedAssociationCollection( collectionName ) &&
					!isGlobalAssociationCollection( collectionName ) ) {
				names.add( collectionName );
			}
		}

		return names;
	}

	private Set<String> getDedicatedAssociationCollections(SessionFactory sessionFactory) {
		MongoDatabase db = MongoDBTestHelper.getProvider( sessionFactory ).getDatabase();
		Set<String> names = new HashSet<>();
		MongoCursor<String> collections = db.listCollectionNames().iterator();
		while ( collections.hasNext() ) {
			String collectionName = collections.next();
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
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( session.getSessionFactory() );
		Document finder = new Document( MongoDBDialect.ID_FIELDNAME, key.getColumnValues()[0] );
		Document result = provider.getDatabase().getCollection( key.getTable() ).find( finder ).first();
		replaceIdentifierColumnName( result, key );
		return DocumentUtil.toMap( result );
	}

	/**
	 * The MongoDB dialect replaces the name of the column identifier, so when the tuple is extracted from the db
	 * we replace the column name of the identifier with the original one.
	 * We are assuming the identifier is not embedded and is a single property.
	 */
	private void replaceIdentifierColumnName(Document result, EntityKey key) {
		Object idValue = result.get( MongoDBDialect.ID_FIELDNAME );
		result.remove( MongoDBDialect.ID_FIELDNAME );
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
			provider.getDatabase().drop();
		}
		catch ( MongoException ex ) {
			throw log.unableToDropDatabase( ex, provider.getDatabase().getName() );
		}
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		//read variables from the System properties set in the static initializer
		Map<String, String> envProps = new HashMap<String, String>( 4 );
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
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new MongoDBDialect( (MongoDBDatastoreProvider) datastoreProvider );
	}

	public static void assertDocument(OgmSessionFactory sessionFactory, String collection, String queryDocument, String expectedDocument) {
		assertDocument( sessionFactory, collection, queryDocument, null, expectedDocument );
	}

	public static void assertDocument(OgmSessionFactory sessionFactory, String collection, String queryDocument, String projectionDocument, String expectedDocument) {
		Document finder =  Document.parse( queryDocument );
		Document fields = projectionDocument != null ? Document.parse( projectionDocument ) : null;

		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		Document actualDocument = provider.getDatabase().getCollection( collection ).find( finder ).projection( fields ).first();

		assertJsonEquals( expectedDocument, actualDocument.toJson() );
	}

	public static Map<String, Document> getIndexes(OgmSessionFactory sessionFactory, String collection) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		MongoCursor<Document> indexes = provider.getDatabase().getCollection( collection ).listIndexes().iterator();
		Map<String, Document> indexMap = new HashMap<>();

		while ( indexes.hasNext() ) {
			Document index = indexes.next();
			indexMap.put( index.get( "name" ).toString(), index );
		}

		return indexMap;
	}


	public static void dropIndexes(OgmSessionFactory sessionFactory, String collection) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		provider.getDatabase().getCollection( collection ).dropIndexes();
	}

	public static void assertJsonEquals(String expectedJson, String actualJson) {
		try {
			JSONCompareResult result = JSONCompare.compareJSON( expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE );

			if ( result.failed() ) {
				throw new AssertionError( result.getMessage() + "; Actual: " + actualJson + "; Expected: " + expectedJson );
			}
		}
		catch (JSONException e) {
			Exceptions.<RuntimeException>sneakyThrow( e );
		}
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return MongoDB.class;
	}
}
