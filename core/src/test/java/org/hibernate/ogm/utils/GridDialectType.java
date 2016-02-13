/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

/**
 * The testsuite needs some knowledge on all NoSQL stores it is meant to support. We mainly need the name of it's
 * TestableGridDialect implementation, but this is also used to disable some tests for a specific GridDialect.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 * @author Gunnar Morling
 */
public enum GridDialectType {

	HASHMAP("org.hibernate.ogm.datastore.map.impl.MapDialect", false, false),

	INFINISPAN("org.hibernate.ogm.datastore.infinispan.InfinispanDialect", false, false),

	EHCACHE("org.hibernate.ogm.datastore.ehcache.EhcacheDialect", false, false),

	MONGODB("org.hibernate.ogm.datastore.mongodb.MongoDBDialect", true, true),

	NEO4J("org.hibernate.ogm.datastore.neo4j.Neo4jDialect", false, true),

	COUCHDB("org.hibernate.ogm.datastore.couchdb.CouchDBDialect", true, false),

	CASSANDRA("org.hibernate.ogm.datastore.cassandra.CassandraDialect", false, false),

	REDIS("org.hibernate.ogm.datastore.redis.RedisDialect", false, false),

	ORIENTDB( "org.hibernate.datastore.ogm.orientdb.OrientDBDialect", false, false  ),
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

	public Class<TestableGridDialect> loadGridDialectClass() {
		return TestHelper.loadClass( dialectClassName );
	}

	/**
	 * Whether this store is a document store or not.
	 * 
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
