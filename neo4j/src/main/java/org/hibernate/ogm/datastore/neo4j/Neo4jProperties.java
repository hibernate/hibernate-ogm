/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Neo4j GridDialect configuration options.
 *
 * @author Davide D'Alto
 * @see org.hibernate.ogm.datastore.neo4j.EmbeddedNeo4jDialect
 */
public final class Neo4jProperties implements OgmProperties {

	/**
	 * The absolute path representing the location of the Neo4j database, ex.: /home/user/neo4jdb/mydb
	 */
	public static final String DATABASE_PATH = "hibernate.ogm.neo4j.database_path";

	/**
	 * Location of the Neo4j embedded properties file. It can be the name of a class path resource, an URL or an
	 * absolute file path.
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.neo4j.configuration_resource_name";

	/**
	 * The maximum number of cached queries used to get a sequence.
	 * <p>
	 * Default is 128.
	 */
	public static final String SEQUENCE_QUERY_CACHE_MAX_SIZE = "hibernate.ogm.neo4j.sequence_query_cache_max_size";

	/**
	 * Socket inactivity timeout in milliseconds.
	 */
	public static String SOCKET_TIMEOUT = "hibernate.ogm.neo4j.client.socket_timeout";

	/**
	 * How long will we wait (in milliseconds) to get a connection?
	 */
	public static final String CONNECTION_CHECKOUT_TIMEOUT = "hibernate.ogm.neo4j.client.connection_checkout_timeout";

	/**
	 * Time to live of the connection in the pool.
	 */
	public static final String CONNECTION_TTL = "hibernate.ogm.neo4j.client.connection_ttl";

	/**
	 * the timeout in millisecond to make an initial socket connection.
	 */
	public static final String ESTABLISH_CONNECTION_TIMEOUT = "hibernate.ogm.neo4j.client.establish_connection_timeout";

	/**
	 * The size of the client connection pool
	 * <p>
	 *     Default value is 10
	 * </p>
	 */
	public static final String CONNECTION_POOL_SIZE = "hibernate.ogm.neo4j.client.connection_pool_size";

	private Neo4jProperties() {
	}
}
