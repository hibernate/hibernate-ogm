/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.infinispan;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Properties for configuring the Infinispan datastore via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 *
 * @author Gunnar Morling
 */
public final class InfinispanProperties implements OgmProperties {

	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 */
	public static final String CONFIGURATION_RESOURCENAME = "hibernate.ogm.infinispan.configuration_resourcename";

	/**
	 * The key for the configuration property to define the JNDI name of the cache manager. If this property is defined,
	 * the cache manager will be looked up via JNDI. JNDI properties passed in the form <tt>hibernate.jndi.*</tt> are
	 * used to define the context properties.
	 */
	public static final String CACHE_MANAGER_RESOURCE_PROP = "hibernate.ogm.infinispan.cachemanager_jndiname";

	private InfinispanProperties() {
	}
}
