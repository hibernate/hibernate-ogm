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

import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.context.PropertyContext;

/**
 * Mapping programmatic API to set NoSQL generic mapping options.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface NoSqlMapping {

	public interface NoSqlGlobalContext<G extends GlobalContext<G, E>, E extends EntityContext<E, ?>> extends GlobalContext<G, E> {
	}

	public interface NoSqlEntityContext<E extends EntityContext<E, P>, P extends PropertyContext<E, P>> extends EntityContext<E, P> {
	}

	public interface NoSqlPropertyContext<E extends EntityContext<E, P>, P extends PropertyContext<E, P>> extends PropertyContext<E, P> {
	}

}
