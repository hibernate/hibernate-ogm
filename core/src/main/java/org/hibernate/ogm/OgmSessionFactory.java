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
package org.hibernate.ogm;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.context.GlobalContext;

/**
 * Provides OGM-specific functionality on the session factory level.
 *
 * @author Gunnar Morling
 */
public interface OgmSessionFactory extends SessionFactoryImplementor {

	/**
	 * Returns a typed {@link GlobalContext} object allowing to apply store-specific configuration options. These
	 * settings are applied to all sessions created via this factory.
	 *
	 * @param datastoreType the configuration type representing the current datastore
	 * @param <G> the returned type of global context
	 * @param <D> the configuration type representing the current datastore
	 * @return a {@link GlobalContext} object
	 * @throws org.hibernate.HibernateException in case the given configuration type isn't supported by the current datastore provider
	 */
	<G extends GlobalContext<G, ?>, D extends DatastoreConfiguration<G>> G configureDatastore(Class<D> datastoreType);
}
