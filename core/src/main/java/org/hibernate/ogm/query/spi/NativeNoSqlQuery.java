/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.query.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.jpa.impl.NoSQLQueryImpl;

/**
 * Represents a query in a store's specific, non-string query format, e.g. a {@code DBObject} in case of MongoDB.
 *
 * @author Gunnar Morling
 */
public class NativeNoSqlQuery<Q> extends NoSQLQueryImpl {

	private final Q query;
	private final SessionImplementor session;

	public NativeNoSqlQuery(Q query, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( query.toString(), session, parameterMetadata );
		this.query = query;
		this.session = session;
	}

	@Override
	public List<?> list() throws HibernateException {
		verifyParameters();
		before();

		List<NativeSQLQueryReturn> queryReturns = getQueryReturns();
		NativeNoSqlQuerySpecification<Q> spec = new NativeNoSqlQuerySpecification<Q>( query, queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ), getSynchronizedQuerySpaces() );
		Map<?, ?> namedParams = getNamedParams();

		try {
			return session.list( spec, getQueryParameters( namedParams ) );
		}
		finally {
			after();
		}
	}

}
