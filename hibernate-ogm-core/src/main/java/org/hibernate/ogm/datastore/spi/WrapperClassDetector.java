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

import java.lang.reflect.InvocationTargetException;
import org.hibernate.HibernateException;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * 
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class WrapperClassDetector {

	private static final Log log = LoggerFactory.make();

	private static enum WrapperClasses {
		JAVA_LANG_BYTE("java.lang.Byte"), JAVA_LANG_SHORT("java.lang.Short"), JAVA_LANG_INTEGER(
				"java.lang.Integer"), JAVA_LANG_LONG("java.lang.Long"), JAVA_LANG_FLOAT(
				"java.lang.Float"), JAVA_LANG_DOUBLE("java.lang.Double"), JAVA_LANG_CHARACTER(
				"java.lang.Character"), JAVA_LANG_BOOLEAN("java.lang.Boolean");

		String className;

		private WrapperClasses(String className) {
			this.className = className;
		}

		/**
		 * Gets the class name of this enum.
		 * 
		 * @return
		 */
		public String getClassName() {
			return this.className;
		}

		/**
		 * Checks if the specified class name is one of the wrapper class names
		 * or not.
		 * 
		 * @param className
		 *            Class name to be examined.
		 * @return True if the specified class name is one of the wrapper class,
		 *         false otherwise.
		 */
		public static boolean isWrapper(String className) {

			for (WrapperClasses clss : values()) {
				if (clss.getClassName().equals(className)) {
					return true;
				}
			}

			return false;
		}
	}

	/**
	 * Checks if the specified class is a wrapper class for the corresponding
	 * primitive type.
	 * 
	 * @param cls
	 *            Class to be examined.
	 * @return True if it's a wrapper class, false otherwise.
	 */
	public boolean isWrapperClass(Class cls) {
		boolean b = WrapperClasses.isWrapper(cls.getCanonicalName());
		return b;
	}

	/**
	 * Casts the specified wrapper source object to the destination class
	 * representation. This method expects the source object is one of the
	 * primitive objects and destination class is also one of the wrapper class.
	 * 
	 * @param <S>
	 *            Source object.
	 * @param <D>
	 *            Destination class.
	 * @param <R>
	 *            Returned object.
	 * @param sourceObject
	 *            Source object to be recreated to the specified destination
	 *            class.
	 * @param destClass
	 *            Used to recreate the source object.
	 * @return Newly recreated source object with the destination class.
	 */
	public <S extends Object, D extends Class, R extends Object> R castWrapperClassFrom(
			S sourceObject, D destClass) {

		if (this.isWrapperClass(sourceObject.getClass())
				&& this.isWrapperClass(destClass)) {

			try {
				R destCls = (R) destClass.getDeclaredConstructor(String.class)
						.newInstance(sourceObject.toString());
				return destCls;
			} catch (IllegalArgumentException e) {
				throw new HibernateException(e);
			} catch (SecurityException e) {
				throw new HibernateException(e);
			} catch (InstantiationException e) {
				throw new HibernateException(e);
			} catch (IllegalAccessException e) {
				throw new HibernateException(e);
			} catch (InvocationTargetException e) {
				throw new HibernateException(e);
			} catch (NoSuchMethodException e) {
				throw new HibernateException(e);
			}
		}

		return null;
	}
}
