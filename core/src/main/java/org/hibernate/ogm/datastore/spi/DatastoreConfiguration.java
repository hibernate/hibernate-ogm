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
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.options.context.GlobalContext;
import org.hibernate.ogm.options.context.impl.ConfigurationContext;

/**
 * Implementations represent a specific datastore to the user and allow to apply store-specific configuration settings.
 * <p>
 * Implementations must provide a no-args constructor.
 *
 * @author Gunnar Morling
 * @param <G> the type of {@link GlobalContext} supported by the represented datastore
 * @see org.hibernate.ogm.cfg.Configurable#configureOptionsFor(Class)
 */
public interface DatastoreConfiguration<G extends GlobalContext<?, ?>> {

	/**
	 * Returns a new store-specific {@link GlobalContext} instance. Used by the Hibernate OGM engine during
	 * bootstrapping a session factory, not intended for client use.
	 *
	 * @param context configuration context to be used as factory for creating the global context object
	 * @return a new {@link GlobalContext}
	 */
	G getConfigurationBuilder(ConfigurationContext context);
}
