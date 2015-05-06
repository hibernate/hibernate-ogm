/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.query.parsing.impl.MongoDBBasedQueryParserService;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;

/**
 * Provides access to a MongoDB instance
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class MongoDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final int AUTHENTICATION_FAILED_CODE = 18;

	private static final Log log = LoggerFactory.getLogger();

	private ServiceRegistryImplementor serviceRegistry;

	private MongoClient mongo;
	private DB mongoDb;
	private MongoDBConfiguration config;

	public MongoDBDatastoreProvider() {
	}

	/**
	 * Only used in tests.
	 *
	 * @param mongoClient the client to connect to mongodb
	 */
	public MongoDBDatastoreProvider(MongoClient mongoClient) {
		this.mongo = mongoClient;
	}

	@Override
	public void configure(Map configurationValues) {
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, classLoaderService );

		try {
			this.config = new MongoDBConfiguration( propertyReader, optionsService.context().getGlobalOptions() );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToConfigureDatastoreProvider( e );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return MongoDBBasedQueryParserService.class;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return MongoDBEntityMappingValidator.class;
	}

	@Override
	public boolean allowsTransactionEmulation() {
		return true;
	}

	@Override
	public void start() {
		try {
			if ( mongo == null ) {
				mongo = createMongoClient( config );
			}
			mongoDb = extractDatabase( mongo, config );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	protected MongoClient createMongoClient(MongoDBConfiguration config) {
		MongoClientOptions clientOptions = config.buildOptions();
		List<MongoCredential> credentials = config.buildCredentials();
		log.connectingToMongo( config.getHost(), config.getPort(), clientOptions.getConnectTimeout() );
		try {
			ServerAddress serverAddress = new ServerAddress( config.getHost(), config.getPort() );
			return credentials == null
					? new MongoClient( serverAddress, clientOptions )
					: new MongoClient( serverAddress, credentials, clientOptions );
		}
		catch (UnknownHostException e) {
			throw log.mongoOnUnknownHost( config.getHost() );
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeMongoDB( e );
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		mongo.close();
	}

	public DB getDatabase() {
		return mongoDb;
	}

	private DB extractDatabase(MongoClient mongo, MongoDBConfiguration config) {
		try {
			String databaseName = config.getDatabaseName();
			log.connectingToMongoDatabase( databaseName );

			Boolean containsDatabase;
			try {
				containsDatabase = mongo.getDatabaseNames().contains( databaseName );
			}
			catch (MongoException me) {
				// we don't have enough privileges, ignore the database creation
				containsDatabase = null;
			}

			if ( containsDatabase != null && containsDatabase == Boolean.FALSE ) {
				if ( config.isCreateDatabase() ) {
					log.creatingDatabase( databaseName );
				}
				else {
					throw log.databaseDoesNotExistException( config.getDatabaseName() );
				}
			}
			DB db = mongo.getDB( databaseName );
			if ( containsDatabase == null ) {
				// force a connection to make sure we do have read access
				// otherwise the connection failure happens during the first flush
				int retries = 0;
				while (true) {
					try {
						db.collectionExists( "WeDoNotCareWhatItIsWeNeedToConnect" );
						break;
					}
					catch (MongoTimeoutException me) {
						// unless we retry twice, the second access will be a TimeoutException error instead of an auth error
						// This is a workaround for https://jira.mongodb.org/browse/JAVA-1803
						retries++;
						if ( retries > 2 ) {
							throw me;
						}
					}
				}
			}
			return mongo.getDB( databaseName );
		}
		catch (MongoException me) {
			switch ( me.getCode() ) {
				case AUTHENTICATION_FAILED_CODE:
					throw log.authenticationFailed( config.getUsername() );
				default:
					throw log.unableToConnectToDatastore( config.getHost(), config.getPort(), me );
			}
		}
	}
}
