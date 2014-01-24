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
package org.hibernate.ogm.datastore.ehcache.impl.configuration;

import java.util.Map;

import org.hibernate.ogm.datastore.ehcache.EhcacheProperties;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class EhcacheConfiguration {

	private String url;

	/**
	 * Initialize the internal values from the given {@link Map}.
	 *
	 * @see Environment
	 * @param configurationMap The values to use as configuration
	 */
	public void initialize(Map configurationMap) {
		this.url = (String) configurationMap.get( EhcacheProperties.RESOURCE_NAME );
	}

	/**
	 * @see Environment#RESOURCE_NAME
	 * @return An URL to an XML file compliant with the ehcache.xsd schema.
	 */
	public String getUrl() {
		return url;
	}
}
