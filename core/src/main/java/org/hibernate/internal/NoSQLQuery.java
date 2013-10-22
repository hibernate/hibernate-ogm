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
package org.hibernate.internal;

import org.hibernate.SQLQuery;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Hibernte OGM implementation of the {@link SQLQuery} contract
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class NoSQLQuery extends SQLQueryImpl implements SQLQuery {

	// The constructor of SQLQueryImpl is package-private
	public NoSQLQuery(String queryString, SessionImplementor sessionImplementor, ParameterMetadata parameterMetadata) {
		super( queryString, sessionImplementor, parameterMetadata );
	}

}
