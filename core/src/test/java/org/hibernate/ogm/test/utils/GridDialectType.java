/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.utils;

/**
 * The testsuite needs some knowledge on all NoSQL stores it is meant to support.
 * We mainly need the name of it's TestableGridDialect implementation, but this
 * is also used to disable some tests for a specific GridDialect.
 *
* @author Sanne Grinovero <sanne@hibernate.org>
* @author Gunnar Morling
*/
public enum GridDialectType {

	HASHMAP( "org.hibernate.ogm.test.utils.HashMapTestHelper", false ) {
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
