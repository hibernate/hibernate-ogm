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

package org.hibernate.ogm.datastore.infinispan.impl.configuration;

/**
 * Configuration options for {@link org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider}
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public interface Environment {
	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 */
	String INFINISPAN_CONFIGURATION_RESOURCENAME = "hibernate.ogm.infinispan.configuration_resourcename";

	/**
	 * The key for the configuration property to define the jndi name of the cachemanager.
	 * If this property is defined, the cachemanager will be looked up via JNDI.
	 * JNDI properties passed in the form <tt>hibernate.jndi.*</tt> are used to define the context properties.
	 */
	String CACHE_MANAGER_RESOURCE_PROP = "hibernate.ogm.infinispan.cachemanager_jndiname";
	String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";
}
