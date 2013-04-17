/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.hibernatecore.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.service.impl.QueryParserService;

/**
 * Custom Query implementation for Hibernate OGM.
 * Splits input of parameter phases from execution, stage at which it will
 * delegate to a different Query as defined by the installed
 * {@link QueryParserService}.
 *
 * Only supports read queries, and without locking options.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class OgmQuery extends AbstractQueryImpl {

	private final Session session;
	private final QueryParserService queryParserService;

	public OgmQuery(String queryString, FlushMode flushMode, OgmSession session,
			ParameterMetadata parameterMetadata, QueryParserService queryParserService) {
		super( queryString, flushMode, session, parameterMetadata );
		this.session = session;
		this.queryParserService = queryParserService;
	}

	@Override
	public Iterator iterate() throws HibernateException {
		return getExecutingQuery().iterate();
	}

	@Override
	public ScrollableResults scroll() throws HibernateException {
		return getExecutingQuery().scroll();
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		return getExecutingQuery().scroll( scrollMode );
	}

	@Override
	public List list() throws HibernateException {
		return getExecutingQuery().list();
	}

	/**
	 * The executing Query is a read-only representation of the current Query,
	 * so it only supports execution and expects all parameters to be set at this point.
	 */
	private Query getExecutingQuery() {
		Map<String,Object> namedParameters = toUntypedParameters();
		return queryParserService.getParsedQueryExecutor( session, getQueryString(), namedParameters );
	}

	/**
	 * In this implementation we don't care for the Type as defined in TypedValue
	 * but leave the responsibility of knowing the proper type to the actual AST walker.
	 * In the case of the Lucene Queries generator, using the Hibernate Search provided
	 * QueryBuilder this should pick the correct converter.
	 */
	private Map<String,Object> toUntypedParameters() {
		return new HashMap<String,Object>( this.getNamedParams() );
	}

	@Override
	public int executeUpdate() throws HibernateException {
		throw new NotSupportedException( "TBD", "QueryQuery#executeUpdate not implemented" );
	}

	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		throw new NotSupportedException( "TBD", "QueryQuery#setLockOptions not implemented" );
	}

	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new NotSupportedException( "TBD", "QueryQuery#setLockMode not implemented" );
	}

	@Override
	public LockOptions getLockOptions() {
		//We could return a default LockOptions, but that would expose mutable setters
		//so better implement those setters first.
		throw new NotSupportedException( "TBD", "AbstractQueryImplQuery#getLockOptions not implemented" );
	}

}
