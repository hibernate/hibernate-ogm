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

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlEntityContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlPropertyContext;
import org.hibernate.ogm.options.spi.Option;

/**
 * Container the common parts of different implementation of {@link EntityContext}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public abstract class NoSqlEntityContextImpl<E extends NoSqlEntityContext<E, P>, P extends NoSqlPropertyContext<E, P>> implements
		NoSqlEntityContext<E, P> {

	private final GlobalContext<?, E> global;
	private final Class<?> type;
	private final MappingContext context;

	public NoSqlEntityContextImpl(MappingContext context, GlobalContext<?, E> global, Class<?> type) {
		this.global = global;
		this.type = type;
		this.context = context;
	}

	@Override
	public E entity(Class<?> type) {
		return global.entity( type );
	}

	@Override
	public abstract P property(String propertyName, ElementType field);

	protected final void addOption(Option<?, ?> option) {
		context.addEntityOption( type, option );
	}

	protected MappingContext context() {
		return context;
	}

	public Class<?> type() {
		return type;
	}

}
