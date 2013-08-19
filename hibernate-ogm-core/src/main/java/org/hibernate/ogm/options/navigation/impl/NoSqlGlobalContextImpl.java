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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.options.generic.NamedQueryOption;
import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlGlobalContext;
import org.hibernate.ogm.options.spi.Option;

/**
 * Container the common parts of different implementation of {@link GlobalContext}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public abstract class NoSqlGlobalContextImpl<G extends GlobalContext<G, E>, E extends EntityContext<E, ?>> implements NoSqlGlobalContext<G, E> {

	private final Map<Class<?>, E> entities = new HashMap<Class<?>, E>();
	private final MappingContext context;

	public NoSqlGlobalContextImpl(MappingContext context) {
		this.context = context;
	}

	public E entity(Class<?> type, E entity) {
		if ( entities.containsKey( type ) ) {
			return entities.get( type );
		}
		else {
			entities.put( type, entity );
			AnnotationProcessor.saveEntityOptions( context, type );
			AnnotationProcessor.savePropertyOptions( context, type );
			return entity;
		}
	}

	protected final void addOption(Option<?, ?> option) {
		context.addGlobalOption( option );
	}

	protected MappingContext context() {
		return context;
	}

	@Override
	public G namedQuery(String name, String hql) {
		addOption( new NamedQueryOption( name, hql ) );
		return (G) this;
	}

}
