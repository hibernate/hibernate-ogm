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
package org.hibernate.ogm.cfg.impl;

import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;


/**
 * @author Gunnar Morling
 *
 */
public class ConfigurableImpl implements Configurable {

	private static final Log log = LoggerFactory.make();
	private final WritableOptionsServiceContext context;

	public ConfigurableImpl() {
		context = new WritableOptionsServiceContext();
	}

	@Override
	public <D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> G configureOptionsFor(Class<D> datastoreType) {
		D configuration = newInstance( datastoreType );
		return configuration.getConfigurationBuilder( new ConfigurationContext( context ) );
	}

	public WritableOptionsServiceContext getContext() {
		return context;
	}

	private <D extends DatastoreConfiguration<?>> D newInstance(Class<D> datastoreType) {
		try {
			return datastoreType.newInstance();

		}
		catch (Exception e) {
			throw log.unableToInstantiateType( datastoreType.getName(), e );
		}
	}
}
