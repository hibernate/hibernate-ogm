/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.impl;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.HostParser;
import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.util.configurationreader.impl.Validators;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * @see DocumentStoreConfiguration
 * @author Davide D'Alto
 */
public class RemoteNeo4jConfiguration {

	public static final int DEFAULT_HTTP_PORT = 7474;

	public static final int DEFAULT_BOLT_PORT = 7687;

	/**
	 * Default Size of the client connection pool
	 */
	public static final int DEFAULT_CONNECTION_POOL_SIZE = 10;

	/**
	 * The default host to connect to in case the {@link OgmProperties#HOST} property is not set
	 */
	private static final String DEFAULT_HOST = "localhost";

	private static final String DEFAULT_DB = "db/data";

	private final Hosts hosts;
	private final String databaseName;
	private final String username;
	private final String password;
	private final boolean createDatabase;
	private final Long socketTimeout;
	private final Long establishConnectionTimeout;
	private final Long connectionCheckoutTimeout;
	private final Long connectionTTL;
	private final boolean authenticationRequired;
	private final Integer clientPoolSize;

	public RemoteNeo4jConfiguration(ConfigurationPropertyReader propertyReader, int defaultPort) {
		String host = propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( DEFAULT_HOST )
				.getValue();

		Integer port =  propertyReader.property( OgmProperties.PORT, Integer.class )
				.withValidator( Validators.PORT )
				.withDefault( null )
				.getValue();

		this.hosts = HostParser.parse( host, port, defaultPort );

		this.databaseName = propertyReader.property( OgmProperties.DATABASE, String.class )
				.withDefault( DEFAULT_DB )
				.getValue();

		this.username = propertyReader.property( OgmProperties.USERNAME, String.class ).getValue();
		this.password = propertyReader.property( OgmProperties.PASSWORD, String.class ).getValue();
		this.socketTimeout = propertyReader.property( Neo4jProperties.SOCKET_TIMEOUT, Long.class ).getValue();
		this.establishConnectionTimeout = propertyReader.property( Neo4jProperties.ESTABLISH_CONNECTION_TIMEOUT, Long.class ).getValue();
		this.connectionCheckoutTimeout = propertyReader.property( Neo4jProperties.CONNECTION_CHECKOUT_TIMEOUT, Long.class ).getValue();
		this.connectionTTL = propertyReader.property( Neo4jProperties.CONNECTION_TTL, Long.class ).getValue();

		this.createDatabase = propertyReader.property( OgmProperties.CREATE_DATABASE, boolean.class )
				.withDefault( false )
				.getValue();
		this.authenticationRequired = this.username != null;
		this.clientPoolSize = propertyReader.property( Neo4jProperties.CONNECTION_POOL_SIZE, Integer.class )
				.withDefault( DEFAULT_CONNECTION_POOL_SIZE ).getValue();
	}

	/**
	 * @see OgmProperties#HOST
	 * @see OgmProperties#PORT
	 * @return The host name of the data store instance
	 */
	public Hosts getHosts() {
		return hosts;
	}

	/**
	 * @see OgmProperties#DATABASE
	 * @return the name of the database to connect to
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @see OgmProperties#USERNAME
	 * @return The user name to be used for connecting with the data store
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @see OgmProperties#PASSWORD
	 * @return The password to be used for connecting with the data store
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @see OgmProperties#CREATE_DATABASE
	 * @return whether to create the database to connect to if not existent or not
	 */
	public boolean isCreateDatabase() {
		return createDatabase;
	}

	/**
	 * @see Neo4jProperties#SOCKET_TIMEOUT
	 * return Socket inactivity timeout in milliseconds
	 */
	public Long getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * @see Neo4jProperties#CONNECTION_CHECKOUT_TIMEOUT
	 * return Socket inactivity timeout in milliseconds
	 */
	public Long getConnectionCheckoutTimeout() {
		return connectionCheckoutTimeout;
	}

	/**
	 * @see Neo4jProperties#CONNECTION_TTL
	 * return the time to live of the connection in the pool.
	 */
	public Long getConnectionTTL() {
		return connectionTTL;
	}

	/**
	 * @see Neo4jProperties#ESTABLISH_CONNECTION_TIMEOUT
	 * @return the timeout in millisecond to make an initial socket connection
	 */
	public Long getEstablishConnectionTimeout() {
		return establishConnectionTimeout;
	}

	/**
	 * @return true if the client needs to authenticate to the server
	 */
	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}

	/**
	 * @see Neo4jProperties#CONNECTION_POOL_SIZE
	 * @return the size of connection pool
	 */
	public Integer getClientPoolSize() {
		return clientPoolSize;
	}
}
