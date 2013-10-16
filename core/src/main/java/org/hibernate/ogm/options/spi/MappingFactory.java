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
package org.hibernate.ogm.options.spi;

import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.hibernate.service.Service;

/**
 * Factory for the creation of {@link GlobalContext}s, providing the entry point into the fluent API for provider-specific
 * configurations, on a global, per-entity and per-property level.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 *
 * @param <G> The type of {@link GlobalContext} created by this factory
 */
public interface MappingFactory<G extends GlobalContext<?, ?>> extends Service {

	/**
	 * Returns a new global configuration context for configuring the current datastore provider
	 *
	 * @return a new global configuration context, of the provider-specific {@link GlobalContext} sub-type
	 */
	G createMapping(OptionsContext context);

}
