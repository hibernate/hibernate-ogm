/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.redis.RedisJsonDialect;
import org.hibernate.ogm.datastore.redis.logging.impl.Log;
import org.hibernate.ogm.datastore.redis.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;

import static com.lambdaworks.redis.RedisURI.Builder.redis;

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
	private AbstractRedisClient redisClient;
	private StatefulConnection<String, String> connection;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return RedisJsonDialect.class;
	}

	@Override
	public void configure(Map configurationValues) {
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
	}

	@Override
	public void start() {
		try {

			if ( config.isCluster() ) {
				RedisClusterClient clusterClient = createClusterClient( config.getHosts() );
				this.redisClient = clusterClient;
				log.connectingToRedis( config.getHosts().toString(), config.getTimeout() );
				connection = clusterClient.connect( new Utf8StringCodec() );
			}
			else {
				RedisClient client = createClient( config.getHosts().getFirst() );
				this.redisClient = client;
				log.connectingToRedis( config.getHosts().toString(), config.getTimeout() );
				connection = client.connect( new Utf8StringCodec() );
			}
		}
		catch (RuntimeException e) {
			// return a ServiceException to be stack trace friendly
			throw log.unableToInitializeRedis( e );
		}
	}

	protected RedisClient createClient(Hosts.HostAndPort hostAndPort) {
		RedisURI builder = configureRedisUri( hostAndPort );
		return RedisClient.create( builder );
	}

	private RedisURI configureRedisUri(Hosts.HostAndPort hostAndPort) {
		RedisURI.Builder builder = redis( hostAndPort.getHost(), hostAndPort.getPort() );
		builder.withSsl( config.isSsl() );
		builder.withDatabase( config.getDatabaseNumber() );

		if ( config.getPassword() != null ) {
			builder.withPassword( config.getPassword() );
		}

		builder.withTimeout( config.getTimeout(), TimeUnit.MILLISECONDS );
		return builder.build();
	}

	protected RedisClusterClient createClusterClient(Hosts hosts) {

		List<RedisURI> redisURIs = new ArrayList<>();
		for ( Hosts.HostAndPort hostAndPort : hosts ) {
			redisURIs.add( configureRedisUri( hostAndPort ) );
		}

		return RedisClusterClient.create( redisURIs );
	}

	@Override
	public void stop() {
		if ( connection != null ) {
			log.disconnectingFromRedis();
			connection.close();
			connection = null;
			shutdownClient();
		}
	}

	protected void shutdownClient() {
		redisClient.shutdown( 100, 100, TimeUnit.MILLISECONDS );
		redisClient = null;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public boolean allowsTransactionEmulation() {
		return true;
	}

	public RedisClusterCommands<String, String> getConnection() {
		if ( connection instanceof StatefulRedisConnection ) {
			return ( (StatefulRedisConnection) connection ).sync();
		}

		if ( connection instanceof StatefulRedisClusterConnection ) {
			return ( (StatefulRedisClusterConnection) connection ).sync();
		}

		throw new IllegalStateException( "Connection type " + connection + " not supported" );

	}

	/**
	 * @return {@code true} if {@link RedisDatastoreProvider} is configured for cluster mode.
	 */
	public boolean isCluster() {
		return config.isCluster();
	}
}
