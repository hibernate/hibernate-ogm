/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.spi;

/**
 * 
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class JSONedClassDetector {

	private static enum JSONedClasses {
		JAVA_UTIL_CALENDAR("java.util.Calendar"), JAVA_UTIL_DATE("java.util.Date");

		private Class cls;

		JSONedClasses(String className) {
			this.cls = this.getClassWith( className );
		}

		/**
		 * Gets the class representation with the specified class name.
		 * 
		 * @param className
		 *            Used to create Class.
		 * @return Class object based on the specified class name.
		 */
		private Class getClassWith(String className) {
			try {
				return Class.forName( className );
			}
			catch ( ClassNotFoundException e ) {
				throw new RuntimeException( e );
			}
		}

		/**
		 * Checks if this class is assignable from the specified class or not.
		 * 
		 * @param cls
		 *            Used to examine the assignability.
		 * @return True if it's assignable, false otherwise.
		 */
		public static boolean canBeAssignableFrom(Class cls) {
			for ( JSONedClasses jsonedClass : values() ) {
				if ( jsonedClass.getEachClass().isAssignableFrom( cls ) ) {
					return true;
				}
			}

			return false;
		}

		public Class getEachClass() {
			return this.cls;
		}
	}

	public boolean isAssignable(Class cls) {
		return JSONedClasses.canBeAssignableFrom( cls );
	}
}
