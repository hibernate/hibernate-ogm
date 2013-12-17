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
package org.hibernate.ogm.options.navigation.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.spi.OptionsService;

/**
 * Provides read and write access to option contexts maintained at the session factory and session level.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
public class OptionsServiceImpl implements OptionsService, ConfigurationBuilderService {

	private final DatastoreProvider datastoreProvider;
	private final WritableOptionsServiceContext globalContext;

	public OptionsServiceImpl(DatastoreProvider datastoreProvider, SessionFactoryImplementor sessionFactoryImplementor) {
		this.datastoreProvider = datastoreProvider;
		this.globalContext = new WritableOptionsServiceContext();
	}

	//OptionsService

	@Override
	public OptionsServiceContext context() {
		return globalContext;
	}

	@Override
	public OptionsServiceContext context(SessionImplementor session) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	//ConfigurationBuilderService

	@Override
	public GlobalContext<?, ?> getConfigurationBuilder() {
		return datastoreProvider.getConfigurationBuilder( new ConfigurationContext( globalContext ) );
	}
}
