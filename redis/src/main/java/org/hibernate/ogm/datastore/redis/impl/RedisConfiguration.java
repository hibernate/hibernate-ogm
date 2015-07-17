/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.HostParser;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.util.configurationreader.impl.Validators;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import com.lambdaworks.redis.RedisURI;

/**
 * @author Mark Paluch
 */
public class RedisConfiguration {

	/**
	 * The default host to connect to in case the {@link OgmProperties#HOST} property is not set
	 */
	private static final String DEFAULT_HOST = "localhost";

	/**
	 * The default timeout (5000ms) in case the {@link RedisProperties#TIMEOUT} property is not set
	 */
	private static final long DEFAULT_TIMEOUT = TimeUnit.MILLISECONDS.toMillis( 5000 );

	private static final int DEFAULT_PORT = 6379;

	private final Hosts hosts;
	private final int databaseNumber;
	private final String password;
	private long timeout = DEFAULT_TIMEOUT;
	private boolean ssl = false;


	public RedisConfiguration(ConfigurationPropertyReader propertyReader) {
		String host = propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( DEFAULT_HOST )
				.getValue();

		Integer port = propertyReader.property( OgmProperties.PORT, Integer.class )
				.withValidator( Validators.PORT )
				.withDefault( null )
				.getValue();

		hosts = HostParser.parse( host, port, DEFAULT_PORT );

		databaseNumber = propertyReader.property( OgmProperties.PORT, Integer.class )
				.withValidator( Validators.PORT )
				.withDefault( RedisURI.DEFAULT_REDIS_PORT )
				.getValue();

		this.password = propertyReader.property( OgmProperties.PASSWORD, String.class ).getValue();

	}

	/**
	 * @return The host name of the data store instance
	 *
	 * @see OgmProperties#HOST
	 * @see OgmProperties#PORT
	 */
	public Hosts getHosts() {
		return hosts;
	}

	/**
	 * @return the number of the database to connect to
	 *
	 * @see OgmProperties#DATABASE
	 */
	public int getDatabaseNumber() {
		return databaseNumber;
	}

	/**
	 * @return The password to be used for connecting with the data store
	 *
	 * @see OgmProperties#PASSWORD
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return The connection/command timeout for interacting with the data store
	 *
	 * @see RedisProperties#TIMEOUT
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * @return Flag, whether to use SSL.
	 *
	 * @see RedisProperties#SSL
	 */
	public boolean isSsl() {
		return ssl;
	}

	/**
	 * Initialize the internal values form the given {@link Map}.
	 *
	 * @param configurationMap The values to use as configuration
	 */
	public void initConfiguration(Map<?, ?> configurationMap) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationMap );

		this.timeout = propertyReader
				.property( RedisProperties.TIMEOUT, Integer.class )
				.withDefault( (int) DEFAULT_TIMEOUT )
				.getValue();

		this.ssl = propertyReader
				.property( RedisProperties.SSL, boolean.class )
				.withDefault( false )
				.getValue();

	}
}
