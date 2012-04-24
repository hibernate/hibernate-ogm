/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
*/
public enum GridDialectType {

	HASHMAP( "org.hibernate.ogm.test.utils.HashMapTestHelper" ) {
		@Override public Class<?> loadTestableGridDialectClass() {
			return null; //this one is special, we want it only as fallback when all others fail
		}
	},

	INFINISPAN( "org.hibernate.ogm.test.utils.InfinispanTestHelper" ),

	EHCACHE( "org.hibernate.ogm.test.utils.EhcacheTestHelper" ),

	MONGODB( "org.hibernate.ogm.test.utils.MongoDBTestHelper" );

	private final String testHelperClassName;

	GridDialectType(String testHelperClassName) {
		this.testHelperClassName = testHelperClassName;
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

	public static GridDialectType valueFromHelperClass(Class<? extends TestableGridDialect> class1) {
		for ( GridDialectType type : values() ) {
			if ( type.testHelperClassName.equals( class1.getName() ) ){
				return type;
			}
		}
		throw new IllegalArgumentException( class1 +
				" is not one of the TestableGridDialect implementation known to " + GridDialectType.class );
	}

}
