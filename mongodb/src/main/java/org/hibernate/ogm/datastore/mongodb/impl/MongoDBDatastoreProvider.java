/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.net.UnknownHostException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
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
import com.mongodb.ServerAddress;

/**
 * Provides access to a MongoDB instance
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class MongoDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.getLogger();

	private ServiceRegistryImplementor serviceRegistry;

	private MongoClient mongo;
	private DB mongoDb;
	private MongoDBConfiguration config;

	public MongoDBDatastoreProvider() {
	}

	/**
	 * Only used in tests.
	 */
	public MongoDBDatastoreProvider(MongoClient mongoClient) {
		this.mongo = mongoClient;
	}

	@Override
	public void configure(Map configurationValues) {
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, classLoaderService );

		this.config = new MongoDBConfiguration( propertyReader, optionsService.context().getGlobalOptions() );
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
	public void start() {
		if ( mongo == null ) {
			try {
				ServerAddress serverAddress = new ServerAddress( config.getHost(), config.getPort() );
				MongoClientOptions clientOptions = config.buildOptions();

				log.connectingToMongo( config.getHost(), config.getPort(), clientOptions.getConnectTimeout() );

				this.mongo = new MongoClient( serverAddress, clientOptions );
			}
			catch ( UnknownHostException e ) {
				throw log.mongoOnUnknownHost( config.getHost() );
			}
			catch ( RuntimeException e ) {
				throw log.unableToInitializeMongoDB( e );
			}
		}
		mongoDb = extractDatabase();
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		this.mongo.close();
	}

	public DB getDatabase() {
		return mongoDb;
	}

	private DB extractDatabase() {
		try {
			if ( config.getUsername() != null ) {
				DB admin = this.mongo.getDB( "admin" );
				boolean auth = admin.authenticate( config.getUsername(), config.getPassword().toCharArray() );
				if ( !auth ) {
					throw log.authenticationFailed( config.getUsername() );
				}
			}
			String databaseName = config.getDatabaseName();
			log.connectingToMongoDatabase( databaseName );

			if ( !this.mongo.getDatabaseNames().contains( databaseName ) ) {
				log.creatingDatabase( databaseName );
			}
			return this.mongo.getDB( databaseName );
		}
		catch ( HibernateException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw log.unableToConnectToDatastore( this.config.getHost(), this.config.getPort(), e );
		}
	}
}
