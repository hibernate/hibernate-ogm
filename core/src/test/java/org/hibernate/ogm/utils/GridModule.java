/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

/**
 * Provides access to the backend specific test helper.
 * <p>
 * In case test helpers should be specific per dialect actually, this could be merged into {@link GridDialectType}.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 * @author Gunnar Morling
 */
public enum GridModule {

	HASHMAP( "org.hibernate.ogm.utils.HashMapTestHelper" ) {

		@Override
		public Class<TestableGridDialect> loadTestableGridDialectClass() {
			return null; //this one is special, we want it only as fallback when all others fail
		}
	},

	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.utils.InfinispanTestHelper" ),
	EHCACHE( "org.hibernate.ogm.datastore.ehcache.utils.EhcacheTestHelper" ),
	MONGODB( "org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper" ),
	NEO4J( "org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper" ),
	COUCHDB( "org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper" ),
	CASSANDRA( "org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper" ),
	REDIS( "org.hibernate.ogm.datastore.redis.utils.RedisTestHelper" ),
	ORIENTDB( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper" );

	private final String testHelperClassName;

	GridModule(String testHelperClassName) {
		this.testHelperClassName = testHelperClassName;
	}

	public Class<TestableGridDialect> loadTestableGridDialectClass() {
		return TestHelper.loadClass( testHelperClassName );
	}
}
