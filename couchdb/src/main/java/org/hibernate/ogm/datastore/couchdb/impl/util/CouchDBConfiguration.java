/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb.impl.util;

import org.hibernate.ogm.dialect.couchdb.Environment;

import java.util.Map;

/**
 * Provides utility methods to access the CouchDB configuration value
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 */
public class CouchDBConfiguration {

	public static final String DEFAULT_COUCHDB_PORT = "5984";
	public static final String LOCALHOST = "localhost";

	private Map configurationValues;

	public void setConfigurationValues(Map configurationValues) {
		this.configurationValues = configurationValues;
	}

	public String getDatabaseHost() {
		return getPropertyValue( Environment.COUCHDB_HOST, LOCALHOST );
	}

	public int getDatabasePort() {
		return Integer.valueOf( getPropertyValue( Environment.COUCHDB_PORT, DEFAULT_COUCHDB_PORT ) );
	}

	public String getDatabaseName() {
		return getPropertyValue( Environment.COUCHDB_DATABASE, null );
	}

	public String getUsername() {
		return getPropertyValue( Environment.COUCHDB_USERNAME, null );
	}

	public String getPassword() {
		return getPropertyValue( Environment.COUCHDB_PASSWORD, null );
	}

	public boolean isDatabaseToBeCreated() {
		return Boolean.valueOf( getPropertyValue( Environment.COUCHDB_CREATE_DATABASE, Boolean.toString( false ) ) );
	}

	public boolean isDatabaseNameConfigured() {
		return isValueConfigured( getDatabaseName() );
	}

	private String getPropertyValue(String propertyKey, String defaultValue) {
		final String value = getConfigurationValue( propertyKey );
		if ( isValueConfigured( value ) ) {
			return value;
		}
		return defaultValue;
	}

	private String getConfigurationValue(String propertyName) {
		return (String) configurationValues.get( propertyName );
	}

	private boolean isValueConfigured(String property) {
		return property != null && !property.trim().equals( "" );
	}
}
