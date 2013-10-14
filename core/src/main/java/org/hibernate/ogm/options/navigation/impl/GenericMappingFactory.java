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

import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.context.PropertyContext;
import org.hibernate.ogm.options.spi.MappingFactory;

/**
 * A {@link MappingFactory} implementation which returns a default {@link GlobalContext} that provides no
 * store-specific options.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class GenericMappingFactory implements MappingFactory<GlobalContext<?, ?>> {

	@Override
	public GlobalContext<?, ?> createMapping(ConfigurationContext context) {
		return context.createGlobalContext( GenericGlobalOptions.class, GenericEntityOptions.class, GenericPropertyOptions.class );
	}

	private abstract static class GenericGlobalOptions extends BaseGlobalContext<GenericGlobalOptions> implements
			GlobalContext<GenericGlobalOptions, GenericEntityOptions> {

		public GenericGlobalOptions(ConfigurationContext context) {
			super( context );
		}
	}

	private abstract static class GenericEntityOptions extends BaseEntityContext<GenericEntityOptions> implements EntityContext<GenericEntityOptions, GenericPropertyOptions> {

		public GenericEntityOptions(ConfigurationContext context) {
			super( context );
		}
	}

	private abstract static class GenericPropertyOptions extends BasePropertyContext<GenericPropertyOptions> implements PropertyContext<GenericEntityOptions, GenericPropertyOptions> {

		public GenericPropertyOptions(ConfigurationContext context) {
			super( context );
		}
	}
}
