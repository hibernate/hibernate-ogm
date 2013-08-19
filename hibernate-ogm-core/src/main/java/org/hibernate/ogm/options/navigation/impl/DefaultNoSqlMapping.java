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

import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlEntityContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlGlobalContext;
import org.hibernate.ogm.options.spi.NoSqlMapping.NoSqlPropertyContext;

/**
 * A generic mapping that works with every provider
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public interface DefaultNoSqlMapping {

	/**
	 * A generic {@link NoSqlGlobalContext} that can be used with every provider
	 */
	public interface DefaultNoSqlGlobalContext extends NoSqlGlobalContext<DefaultNoSqlGlobalContext, DefaultNoSqlEntityContext> {
	}

	/**
	 * A generic {@link NoSqlEntityContext} that can be used with every provider
	 */
	public interface DefaultNoSqlEntityContext extends NoSqlEntityContext<DefaultNoSqlEntityContext, DefaultNoSqlPropertyContext> {
	}

	/**
	 * A generic {@link NoSqlPropertyContext} that can be used with every provider
	 */
	public interface DefaultNoSqlPropertyContext extends NoSqlPropertyContext<DefaultNoSqlEntityContext, DefaultNoSqlPropertyContext> {
	}

}
