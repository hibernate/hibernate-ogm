/* 
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

package org.hibernate.ogm.datastore.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public abstract class AbstractDatastoreProvider implements DatastoreProvider, Startable, Stoppable {

	private Map<String, String> requiredProperties;

	private static enum RequiredProp {

		PROVIDER("provider", "hibernate.ogm.datastore.provider"), DIALECT("dialect", "hibernate.dialect"), PROVIDER_URL(
				"provider_url", "hibernate.ogm.datastore.provider_url");

		private String name;
		private String propPath;

		RequiredProp(String name, String propPath) {
			this.name = name;
			this.propPath = propPath;
		}

		public String getName() {
			return this.name;
		}

		public String getPropPath() {
			return this.propPath;
		}
	}

	/**
	 * Gets the common required property values among other datastore provider
	 * and the datastore specific property values.
	 * 
	 * @return Key value pairs storing the properties.
	 */
	private Map<String, String> getRequiredPropertyValues() {
		Map<String, String> map = new HashMap<String, String>();
		map.put( RequiredProp.PROVIDER.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER.getPropPath() ) );
		map.put( RequiredProp.DIALECT.getName(), RequiredProp.DIALECT.getPropPath() );
		map.put( RequiredProp.PROVIDER_URL.getName(),
				Environment.getProperties().getProperty( RequiredProp.PROVIDER_URL.getPropPath() ) );
		map.putAll( this.getSpecificSettings() );
		return Collections.unmodifiableMap( map );
	}

	/**
	 * Gets the specific setting values. Currently there is only one
	 * settings for the store name.
	 * 
	 * @return Key-value pair for the specific setting.
	 */
	protected abstract Map<String, String> getSpecificSettings();

	/**
	 * Checks required property settings for Voldemort on hibernate.properties.
	 * 
	 * @return True if all the required properties are set, false otherwise.
	 */
	protected boolean checkRequiredSettings() {
		this.requiredProperties = this.getRequiredPropertyValues();

		if ( this.requiredProperties.get( RequiredProp.PROVIDER.getName() ).equals( this.getClass().getCanonicalName() )
				&& this.requiredProperties.get( RequiredProp.PROVIDER_URL.getName() ) != null
				&& this.requiredProperties.get( RequiredProp.DIALECT.getName() ) != null ) {
			return true;
		}
		return false;
	}

	protected Map<String, String> getRequiredProperties() {
		return this.requiredProperties;
	}

	/**
	 * Creates a wrapper object using the specified primitive class and string
	 * value. This method calls a constructor with string parameter.
	 * 
	 * @param prmitiveClass
	 *            Class used to find the corresponding wrapper class.
	 * @param paramString
	 *            Set in the wrapper class constructor.
	 * @return Wrapper class object or null.
	 */
	protected Object createWrapperClassObjFrom(Class prmitiveClass, String paramString) {
		Class wrapperClass = ClassUtils.primitiveToWrapper( prmitiveClass );
		Constructor ctor;
		try {
			ctor = wrapperClass.getDeclaredConstructor( String.class );
			return ctor.newInstance( paramString );
		}
		catch ( SecurityException e ) {
			this.throwHibernateExceptionFrom( e );
		}
		catch ( NoSuchMethodException e ) {
			this.throwHibernateExceptionFrom( e );
		}
		catch ( IllegalArgumentException e ) {
			this.throwHibernateExceptionFrom( e );
		}
		catch ( InstantiationException e ) {
			this.throwHibernateExceptionFrom( e );
		}
		catch ( IllegalAccessException e ) {
			this.throwHibernateExceptionFrom( e );
		}
		catch ( InvocationTargetException e ) {
			this.throwHibernateExceptionFrom( e );
		}

		return null;
	}

	/**
	 * Converts the specified exception to HibernateException and rethrows it.
	 * 
	 * @param <T>
	 * @param exception
	 *            Exception to be rethrown as HibernateException.
	 */
	protected <T extends Throwable> void throwHibernateExceptionFrom(T exception) {
		throw new HibernateException( exception.getCause() );
	}
}
