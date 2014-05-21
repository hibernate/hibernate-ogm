/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.ogm.OgmSession;
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

	private final OgmSession session;
	private final QueryParserService queryParserService;
	private final LockOptions lockOptions = new LockOptions();

	public OgmQuery(String queryString, FlushMode flushMode, OgmSessionImpl session,
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
	public List<?> list() throws HibernateException {
		Query executingQuery = getExecutingQuery();

		if ( getFirstResult() != null ) {
			executingQuery.setFirstResult( getFirstResult() );
		}
		if ( getMaxResults() != null ) {
			executingQuery.setMaxResults( getMaxResults() );
		}
		return executingQuery.list();
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
	public Query setLockMode(String alias, LockMode lockMode) {
		lockOptions.setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	@Override
	public Query setLockOptions(LockOptions lockOption) {
		this.lockOptions.setLockMode( lockOption.getLockMode() );
		this.lockOptions.setScope( lockOption.getScope() );
		this.lockOptions.setTimeOut( lockOption.getTimeOut() );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

}
