/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.HostParser;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.datastore.redis.logging.impl.Log;
import org.hibernate.ogm.datastore.redis.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;

/**
 * Configuration for {@link RedisDatastoreProvider}.
 *
 * @author Mark Paluch
 */
@Immutable
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
	private static final int DEFAULT_DATABASE = 0;

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * A {@link PropertyValidator} which asserts that a given number is a valid database number.
	 */
	private static final PropertyValidator<Integer> DATABASE_VALIDATOR = new PropertyValidator<Integer>() {

		@Override
		public void validate(Integer value) throws HibernateException {
			if ( value == null ) {
				return;
			}
			if ( value < 0 || value > 15 ) {
				throw log.illegalDatabaseValue( value );
			}
		}
	};

	private final Hosts hosts;
	private final int databaseNumber;
	private final String password;
	private final long timeout;
	private final boolean ssl;
	private final boolean cluster;

	public RedisConfiguration(ConfigurationPropertyReader propertyReader) {
		String host = propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( DEFAULT_HOST )
				.getValue();

		this.hosts = HostParser.parse( host, null, DEFAULT_PORT );

		this.databaseNumber = propertyReader.property( OgmProperties.DATABASE, Integer.class )
				.withValidator( DATABASE_VALIDATOR )
				.withDefault( DEFAULT_DATABASE )
				.getValue();

		this.password = propertyReader.property( OgmProperties.PASSWORD, String.class ).getValue();

		this.timeout = propertyReader
				.property( RedisProperties.TIMEOUT, Integer.class )
				.withDefault( (int) DEFAULT_TIMEOUT )
				.getValue();

		this.ssl = propertyReader
				.property( RedisProperties.SSL, boolean.class )
				.withDefault( false )
				.getValue();

		this.cluster = propertyReader
				.property( RedisProperties.CLUSTER, boolean.class )
				.withDefault( false )
				.getValue();
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
	 * @return Flag, whether to use Redis Cluster.
	 *
	 * @see RedisProperties#CLUSTER
	 */
	public boolean isCluster() {
		return cluster;
	}
}
