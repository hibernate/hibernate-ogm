/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

/**
 * The testsuite needs some knowledge on all NoSQL stores it is meant to support.
 * We mainly need the name of it's TestableGridDialect implementation, but this
 * is also used to disable some tests for a specific GridDialect.
 *
* @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
* @author Gunnar Morling
*/
public enum GridDialectType {

	HASHMAP( "org.hibernate.ogm.utils.HashMapTestHelper", false ) {
		@Override public Class<?> loadTestableGridDialectClass() {
			return null; //this one is special, we want it only as fallback when all others fail
		}
	},

	INFINISPAN( "org.hibernate.ogm.datastore.infinispan.utils.InfinispanTestHelper", false ),

	EHCACHE( "org.hibernate.ogm.datastore.ehcache.utils.EhcacheTestHelper", false ),

	MONGODB( "org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper", true ),

	NEO4J( "org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper", false ),

	COUCHDB( "org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper", true );

	private final String testHelperClassName;
	private final boolean isDocumentStore;

	GridDialectType(String testHelperClassName, boolean isDocumentStore) {
		this.testHelperClassName = testHelperClassName;
		this.isDocumentStore = isDocumentStore;
	}

	public Class<?> loadTestableGridDialectClass() {
		Class<?> classForName = null;
		try {
			classForName = Class.forName( testHelperClassName );
		}
		catch (ClassNotFoundException e) {
			//ignore this: might not be available
		}
		return classForName;
	}

	/**
	 * Whether this store is a document store or not.
	 * @return {@code true} if this is a document store, {@code false} otherwise.
	 */
	public boolean isDocumentStore() {
		return isDocumentStore;
	}

	public static GridDialectType valueFromHelperClass(Class<? extends TestableGridDialect> class1) {
		for ( GridDialectType type : values() ) {
			if ( type.testHelperClassName.equals( class1.getName() ) ) {
				return type;
			}
		}
		throw new IllegalArgumentException( class1 +
				" is not one of the TestableGridDialect implementation known to " + GridDialectType.class );
	}

}
