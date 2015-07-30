/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.redis.RedisDialect;
import org.hibernate.ogm.datastore.redis.logging.impl.Log;
import org.hibernate.ogm.datastore.redis.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;

/**
 * Provides access to Redis
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Mark Paluch
 */
public class RedisDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable,
		ServiceRegistryAwareService, Configurable {

	private static final Log log = LoggerFactory.getLogger();
	private ServiceRegistryImplementor serviceRegistry;

	private RedisConfiguration config;
	private RedisClient redisClient;
	private RedisConnection<byte[], byte[]> connection;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return RedisDialect.class;
	}

	@Override
	public void configure(Map configurationValues) {

		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader(
				configurationValues,
				classLoaderService
		);

		try {
			this.config = new RedisConfiguration( propertyReader );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToConfigureDatastoreProvider( e );
		}

		this.config.initConfiguration( configurationValues );
	}


	@Override
	public void start() {
		try {
			Hosts.HostAndPort hostAndPort = config.getHosts().getFirst();
			RedisURI.Builder builder = new RedisURI.Builder().redis( hostAndPort.getHost(), hostAndPort.getPort() );
			builder.withSsl( config.isSsl() );

			if ( config.getPassword() != null ) {
				builder.withPassword( config.getPassword() );
			}

			builder.withTimeout( config.getTimeout(), TimeUnit.MILLISECONDS );
			redisClient = new RedisClient( builder.build() );

			log.connectingToRedis( config.getHosts().toString(), config.getTimeout() );
			connection = redisClient.connect( new ByteArrayCodec() );

		}
		catch (RuntimeException e) {
			// return a ServiceException to be stack trace friendly
			throw log.unableToInitializeRedis( e );
		}
	}

	@Override
	public void stop() {
		if ( connection != null ) {
			log.disconnectingFromRedis();
			connection.close();
			redisClient.shutdown( 100, 100, TimeUnit.MILLISECONDS );
			connection = null;
			redisClient = null;
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public RedisConnection<byte[], byte[]> getConnection() {
		return connection;
	}
}
