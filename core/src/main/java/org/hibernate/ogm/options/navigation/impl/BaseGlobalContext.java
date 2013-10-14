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

import org.hibernate.ogm.options.generic.NamedQueryOption;
import org.hibernate.ogm.options.spi.GlobalOptions;

/**
 * Base implementation for options declared by {@link GlobalOptions}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public abstract class BaseGlobalContext<G extends GlobalOptions<G>> extends BaseContext implements GlobalOptions<G> {

	public BaseGlobalContext(ConfigurationContext context) {
		super( context );
	}

	@Override
	public G namedQuery(String name, String hql) {
		addGlobalOption( new NamedQueryOption( name, hql ) );

		// safe unless a provider developer screwed up the specific context definition
		@SuppressWarnings("unchecked")
		G globalContext = (G) this;

		return globalContext;
	}
}
