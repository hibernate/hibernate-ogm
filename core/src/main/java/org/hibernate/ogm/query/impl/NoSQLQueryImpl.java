/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractQueryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.type.Type;

/**
 * Hibernate OGM implementation of the {@link SQLQuery} contract.
 * <p>
 * This class is a copy of {@link org.hibernate.internal.SQLQueryImpl} and it is needed because
 * the constructors in of {@code SQlQueryImpl} are package-private and they cannot be called when
 * our application is deployed on WildFly.
 * <p>
 * It also has a better name for a class dealing with NoSQL databases.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NoSQLQueryImpl extends AbstractQueryImpl implements NoSQLQuery {

	private List<NativeSQLQueryReturn> queryReturns;
	private List<ReturnBuilder> queryReturnBuilders;
	private boolean autoDiscoverTypes;

	private Collection<String> querySpaces;

	private final boolean callable;
	private final LockOptions lockOptions = new LockOptions();
	private final SessionImplementor session;
	private Object queryObject;

	/**
	 * Constructs a NoSQLQuery given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined query.
	 * @param session The session to which this NoSQLQuery belongs.
	 * @param parameterMetadata Metadata about parameters found in the query.
	 */
	public NoSQLQueryImpl(NamedSQLQueryDefinition queryDef, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( queryDef.getQueryString(), queryDef.getFlushMode(), session, parameterMetadata );
		this.session = session;
		if ( queryDef.getResultSetRef() != null ) {
			ResultSetMappingDefinition definition = session.getFactory().getResultSetMapping( queryDef.getResultSetRef() );
			if ( definition == null ) {
				throw new MappingException( "Unable to find resultset-ref definition: " + queryDef.getResultSetRef() );
			}
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>( Arrays.asList( definition.getQueryReturns() ) );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>( Arrays.asList( queryDef.getQueryReturns() ) );
		}
		else {
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>();
		}

		this.querySpaces = queryDef.getQuerySpaces();
		this.callable = queryDef.isCallable();
	}

	public NoSQLQueryImpl(String sql, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( sql, false, session, parameterMetadata );
	}

	public NoSQLQueryImpl(Object queryObject, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( queryObject.toString(), false, session, parameterMetadata );
		this.queryObject = queryObject;
	}

	public NoSQLQueryImpl(String sql, boolean callable, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( sql, null, session, parameterMetadata );
		this.session = session;
		this.queryReturns = new ArrayList<NativeSQLQueryReturn>();
		this.querySpaces = null;
		this.callable = callable;
	}

	@Override
	public List<NativeSQLQueryReturn> getQueryReturns() {
		prepareQueryReturnsIfNecessary();
		return queryReturns;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return querySpaces;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public List<?> list() throws HibernateException {
		verifyParameters();
		before();

		Map<?, ?> namedParams = getNamedParams();
		NativeNoSqlQuerySpecification spec = generateQuerySpecification( namedParams );

		try {
			return session.list( spec, getQueryParameters( namedParams ) );
		}
		finally {
			after();
		}
	}

	protected NativeNoSqlQuerySpecification generateQuerySpecification(Map namedParams) {
		if ( queryObject != null ) {
			return new NativeNoSqlQuerySpecification(
					queryObject,
					queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ),
					querySpaces
			);
		}
		else {
			return new NativeNoSqlQuerySpecification(
					expandParameterLists( namedParams ),
					queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ),
					querySpaces
			);
		}
	}

	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		before();

		Map<?, ?> namedParams = getNamedParams();
		NativeNoSqlQuerySpecification spec = generateQuerySpecification( namedParams );

		QueryParameters qp = getQueryParameters( namedParams );
		qp.setScrollMode( scrollMode );

		try {
			return session.scroll( spec, qp );
		}
		finally {
			after();
		}
	}

	@Override
	public ScrollableResults scroll() throws HibernateException {
		return scroll( session.getFactory().getDialect().defaultScrollMode() );
	}

	@Override
	public Iterator<?> iterate() throws HibernateException {
		throw new UnsupportedOperationException( "SQL queries do not currently support iteration" );
	}

	@Override
	public QueryParameters getQueryParameters(Map namedParams) {
		QueryParameters qp = super.getQueryParameters( namedParams );
		qp.setCallable( callable );
		qp.setAutoDiscoverScalarTypes( autoDiscoverTypes );
		return qp;
	}

	@Override
	protected void verifyParameters() {
		// verifyParameters is called at the start of all execution type methods, so we use that here to perform
		// some preparation work.
		prepareQueryReturnsIfNecessary();
		verifyParameters( callable );
		boolean noReturns = queryReturns == null || queryReturns.isEmpty();
		if ( noReturns ) {
			this.autoDiscoverTypes = noReturns;
		}
		else {
			for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
				if ( queryReturn instanceof NativeSQLQueryScalarReturn ) {
					NativeSQLQueryScalarReturn scalar = (NativeSQLQueryScalarReturn) queryReturn;
					if ( scalar.getType() == null ) {
						autoDiscoverTypes = true;
						break;
					}
				}
				else if ( NativeSQLQueryConstructorReturn.class.isInstance( queryReturn ) ) {
					autoDiscoverTypes = true;
					break;
				}
			}
		}
	}

	private void prepareQueryReturnsIfNecessary() {
		if ( queryReturnBuilders != null ) {
			if ( !queryReturnBuilders.isEmpty() ) {
				if ( queryReturns != null ) {
					queryReturns.clear();
					queryReturns = null;
				}
				queryReturns = new ArrayList<NativeSQLQueryReturn>();
				for ( ReturnBuilder builder : queryReturnBuilders ) {
					queryReturns.add( builder.buildReturn() );
				}
				queryReturnBuilders.clear();
			}
			queryReturnBuilders = null;
		}
	}

	@Override
	public String[] getReturnAliases() throws HibernateException {
		throw new UnsupportedOperationException( "SQL queries do not currently support returning aliases" );
	}

	@Override
	public Type[] getReturnTypes() throws HibernateException {
		throw new UnsupportedOperationException( "not yet implemented for SQL queries" );
	}

	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "cannot set the lock mode for a native SQL query" );
	}

	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "cannot set lock options for a native SQL query" );
	}

	@Override
	public LockOptions getLockOptions() {
		// we never need to apply locks to the SQL, however the native-sql loader handles this specially
		return lockOptions;
	}

	@Override
	public SQLQuery addScalar(final String columnAlias, final Type type) {
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( new ReturnBuilder() {

			@Override
			public NativeSQLQueryReturn buildReturn() {
				return new NativeSQLQueryScalarReturn( columnAlias, type );
			}
		} );
		return this;
	}

	@Override
	public SQLQuery addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}

	@Override
	public RootReturn addRoot(String tableAlias, String entityName) {
		RootReturnBuilder builder = new RootReturnBuilder( tableAlias, entityName );
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( builder );
		return builder;
	}

	@Override
	public RootReturn addRoot(String tableAlias, Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}

	@Override
	public SQLQuery addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}

	@Override
	public SQLQuery addEntity(String alias, String entityName) {
		addRoot( alias, entityName );
		return this;
	}

	@Override
	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode) {
		addRoot( alias, entityName ).setLockMode( lockMode );
		return this;
	}

	@Override
	public SQLQuery addEntity(Class entityType) {
		return addEntity( entityType.getName() );
	}

	@Override
	public SQLQuery addEntity(String alias, Class entityClass) {
		return addEntity( alias, entityClass.getName() );
	}

	@Override
	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode) {
		return addEntity( alias, entityClass.getName(), lockMode );
	}

	@Override
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		FetchReturnBuilder builder = new FetchReturnBuilder( tableAlias, ownerTableAlias, joinPropertyName );
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( builder );
		return builder;
	}

	@Override
	public SQLQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}

	@Override
	public SQLQuery addJoin(String alias, String path) {
		createFetchJoin( alias, path );
		return this;
	}

	private FetchReturn createFetchJoin(String tableAlias, String path) {
		int loc = path.indexOf( '.' );
		if ( loc < 0 ) {
			throw new QueryException( "not a property path: " + path );
		}
		final String ownerTableAlias = path.substring( 0, loc );
		final String joinedPropertyName = path.substring( loc + 1 );
		return addFetch( tableAlias, ownerTableAlias, joinedPropertyName );
	}

	@Override
	public SQLQuery addJoin(String alias, String path, LockMode lockMode) {
		createFetchJoin( alias, path ).setLockMode( lockMode );
		return this;
	}

	@Override
	public SQLQuery setResultSetMapping(String name) {
		ResultSetMappingDefinition mapping = session.getFactory().getResultSetMapping( name );
		if ( mapping == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}
		NativeSQLQueryReturn[] returns = mapping.getQueryReturns();
		queryReturns.addAll( Arrays.asList( returns ) );
		return this;
	}

	@Override
	public SQLQuery addSynchronizedQuerySpace(String querySpace) {
		if ( querySpaces == null ) {
			querySpaces = new ArrayList<String>();
		}
		querySpaces.add( querySpace );
		return this;
	}

	@Override
	public SQLQuery addSynchronizedEntityName(String entityName) {
		return addQuerySpaces( session.getFactory().getEntityPersister( entityName ).getQuerySpaces() );
	}

	@Override
	public SQLQuery addSynchronizedEntityClass(Class entityClass) {
		return addQuerySpaces( session.getFactory().getEntityPersister( entityClass.getName() ).getQuerySpaces() );
	}

	private SQLQuery addQuerySpaces(Serializable[] spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<String>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
		return this;
	}

	@Override
	public int executeUpdate() throws HibernateException {
		Map<?, ?> namedParams = getNamedParams();
		before();
		try {
			return session.executeNativeUpdate( generateQuerySpecification( namedParams ), getQueryParameters( namedParams ) );
		}
		finally {
			after();
		}
	}

	public Object getQueryObject() {
		return queryObject;
	}

	private class RootReturnBuilder implements RootReturn, ReturnBuilder {

		private final String alias;
		private final String entityName;
		private LockMode lockMode = LockMode.READ;
		private Map<String, String[]> propertyMappings;

		private RootReturnBuilder(String alias, String entityName) {
			this.alias = alias;
			this.entityName = entityName;
		}

		@Override
		public RootReturn setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}

		@Override
		public RootReturn setDiscriminatorAlias(String alias) {
			addProperty( "class", alias );
			return this;
		}

		@Override
		public RootReturn addProperty(String propertyName, String columnAlias) {
			addProperty( propertyName ).addColumnAlias( columnAlias );
			return this;
		}

		@Override
		public ReturnProperty addProperty(final String propertyName) {
			if ( propertyMappings == null ) {
				propertyMappings = new HashMap<String, String[]>();
			}
			return new ReturnProperty() {

				@Override
				public ReturnProperty addColumnAlias(String columnAlias) {
					String[] columnAliases = propertyMappings.get( propertyName );
					if ( columnAliases == null ) {
						columnAliases = new String[] { columnAlias };
					}
					else {
						String[] newColumnAliases = new String[columnAliases.length + 1];
						System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
						newColumnAliases[columnAliases.length] = columnAlias;
						columnAliases = newColumnAliases;
					}
					propertyMappings.put( propertyName, columnAliases );
					return this;
				}
			};
		}

		@Override
		public NativeSQLQueryReturn buildReturn() {
			return new NativeSQLQueryRootReturn( alias, entityName, propertyMappings, lockMode );
		}
	}

	private class FetchReturnBuilder implements FetchReturn, ReturnBuilder {

		private final String alias;
		private final String ownerTableAlias;
		private final String joinedPropertyName;
		private LockMode lockMode = LockMode.READ;
		private Map<String, String[]> propertyMappings;

		private FetchReturnBuilder(String alias, String ownerTableAlias, String joinedPropertyName) {
			this.alias = alias;
			this.ownerTableAlias = ownerTableAlias;
			this.joinedPropertyName = joinedPropertyName;
		}

		@Override
		public FetchReturn setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}

		@Override
		public FetchReturn addProperty(String propertyName, String columnAlias) {
			addProperty( propertyName ).addColumnAlias( columnAlias );
			return this;
		}

		@Override
		public ReturnProperty addProperty(final String propertyName) {
			if ( propertyMappings == null ) {
				propertyMappings = new HashMap<String, String[]>();
			}
			return new ReturnProperty() {

				@Override
				public ReturnProperty addColumnAlias(String columnAlias) {
					String[] columnAliases = propertyMappings.get( propertyName );
					if ( columnAliases == null ) {
						columnAliases = new String[] { columnAlias };
					}
					else {
						String[] newColumnAliases = new String[columnAliases.length + 1];
						System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
						newColumnAliases[columnAliases.length] = columnAlias;
						columnAliases = newColumnAliases;
					}
					propertyMappings.put( propertyName, columnAliases );
					return this;
				}
			};
		}

		@Override
		public NativeSQLQueryReturn buildReturn() {
			return new NativeSQLQueryJoinReturn( alias, ownerTableAlias, joinedPropertyName, propertyMappings, lockMode );
		}
	}

	private interface ReturnBuilder {

		NativeSQLQueryReturn buildReturn();
	}

}
