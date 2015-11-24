/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;


import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;

/**
 * The testsuite needs some knowledge on all NoSQL stores it is meant to support.
 * We mainly need the name of it's TestableGridDialect implementation, but this
 * is also used to disable some tests for a specific GridModule.
 * This class is a registry for the known testable dialects and providers.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 * @author Gunnar Morling
 */
public enum GridModule {

	HASHMAP( "org.hibernate.ogm.utils.HashMapTestHelper", false, false ) {

		@Override
		public Class<TestableGridDialect> loadTestableGridDialectClass() {
			return null; //this one is special, we want it only as fallback when all others fail
		}

		public GridDialectType dialect() {
			return GridDialectType.HASHMAP;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.MAP;
		}
	},

	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.utils.InfinispanTestHelper", false, false ) {
		public GridDialectType dialect() {
			return GridDialectType.INFINISPAN;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.INFINISPAN;
		}
	},

	EHCACHE( "org.hibernate.ogm.datastore.ehcache.utils.EhcacheTestHelper", false, false ) {
		public GridDialectType dialect() {
			return GridDialectType.EHCACHE;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.EHCACHE;
		}
	},

	MONGODB( "org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper", true, true ) {
		public GridDialectType dialect() {
			return GridDialectType.MONGODB;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.MONGODB;
		}

		public AvailableDatastoreProvider fongoProvider() {
			return AvailableDatastoreProvider.FONGO;
		}
	},

	NEO4J( "org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper", false, true ) {
		public GridDialectType dialect() {
			return GridDialectType.NEO4J;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.NEO4J_EMBEDDED;
		}
	},

	COUCHDB( "org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper", true, false ) {
		public GridDialectType dialect() {
			return GridDialectType.COUCHDB;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.COUCHDB_EXPERIMENTAL;
		}
	},

	CASSANDRA( "org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper", false, false ) {
		public GridDialectType dialect() {
			return GridDialectType.CASSANDRA;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.CASSANDRA_EXPERIMENTAL;
		}
	},

	REDIS( "org.hibernate.ogm.datastore.redis.utils.RedisTestHelper", false, false ) {
		public GridDialectType dialect() {
			return GridDialectType.REDIS;
		}

		public AvailableDatastoreProvider provider() {
			return AvailableDatastoreProvider.REDIS_EXPERIMENTAL;
		}
	};

	private final String testHelperClassName;
	private final boolean isDocumentStore;
	private final boolean supportsQueries;

	GridModule(String testHelperClassName, boolean isDocumentStore, boolean supportsQueries) {
		this.testHelperClassName = testHelperClassName;
		this.isDocumentStore = isDocumentStore;
		this.supportsQueries = supportsQueries;
	}

	public Class<TestableGridDialect> loadTestableGridDialectClass() {
		return TestHelper.loadClass( testHelperClassName );
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

	/**
	 * @return the default provider type for this grid module.
	 */
	public abstract AvailableDatastoreProvider provider();

	/**
	 * @return the default dialect type for this module.
	 */
	public abstract GridDialectType dialect();
}
