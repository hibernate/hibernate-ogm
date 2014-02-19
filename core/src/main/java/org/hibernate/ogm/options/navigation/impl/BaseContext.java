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

import org.hibernate.ogm.options.spi.Option;

/**
 * Base class for {@link org.hibernate.ogm.options.navigation.GlobalContext},
 * {@link org.hibernate.ogm.options.navigation.EntityContext} and
 * {@link org.hibernate.ogm.options.navigation.PropertyContext} implementations which allows to add options for
 * the different kinds of context.
 *
 * @author Gunnar Morling
 */
public class BaseContext {

	private final ConfigurationContext context;

	public BaseContext(ConfigurationContext context) {
		this.context = context;
	}

	protected final <V> void addGlobalOption(Option<?, V> option, V value) {
		context.addGlobalOption( option, value );
	}

	protected final <V> void addEntityOption(Option<?, V> option, V value) {
		context.addEntityOption( option, value );
	}

	protected final <V> void addPropertyOption(Option<?, V> option, V value) {
		context.addPropertyOption( option, value );
	}
}
