/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * The test suite needs some knowledge on all NoSQL stores it is meant to support.
 * This is mainly used to disable some tests for a specific GridDialect.
 *
* @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
* @author Gunnar Morling
*/
public enum GridDialectType {

	HASHMAP( "org.hibernate.ogm.datastore.map.impl.MapDialect", false, false ),

	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.InfinispanDialect", false, false),

	INFINISPAN_REMOTE( "org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect", true, false ),

	EHCACHE( "org.hibernate.ogm.datastore.ehcache.EhcacheDialect", false, false ),

	MONGODB( "org.hibernate.ogm.datastore.mongodb.MongoDBDialect", true, true ),

	NEO4J_EMBEDDED( "org.hibernate.ogm.datastore.neo4j.EmbeddedNeo4jDialect", false, true),

	NEO4J_REMOTE( "org.hibernate.ogm.datastore.neo4j.RemoteNeo4jDialect", false, true),

	COUCHDB( "org.hibernate.ogm.datastore.couchdb.CouchDBDialect", true, false ),

	CASSANDRA( "org.hibernate.ogm.datastore.cassandra.CassandraDialect", false, false  ),

	IGNITE( "org.hibernate.ogm.datastore.ignite.IgniteDialect", false, false  ),

	REDIS_JSON( "org.hibernate.ogm.datastore.redis.RedisJsonDialect", false, false ),

	REDIS_HASH( "org.hibernate.ogm.datastore.redis.RedisHashDialect", false, false );

	private final String dialectClassName;
	private final boolean isDocumentStore;
	private final boolean supportsQueries;

	GridDialectType(String dialectClassName, boolean isDocumentStore, boolean supportsQueries) {
		this.dialectClassName = dialectClassName;
		this.isDocumentStore = isDocumentStore;
		this.supportsQueries = supportsQueries;
	}

	public Class<? extends GridDialect> loadGridDialectClass() {
		return TestHelper.loadClass( dialectClassName );
	}

	/**
	 * Whether this store is a document store or not.
	 * @return {@code true} if this is a document store, {@code false} otherwise.
	 */
	public boolean isDocumentStore() {
		return isDocumentStore;
	}

	/**
	 * Whether this store supports the execution of queries or not.
	 *
	 * @return {@code true} if this store has its own query backend, {@code false} if it uses Hibernate Search for query
	 * execution.
	 */
	public boolean supportsQueries() {
		return supportsQueries;
	}
}
