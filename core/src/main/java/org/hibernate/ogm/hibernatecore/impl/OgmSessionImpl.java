/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.SessionOperations;
import org.hibernate.ogm.datastore.spi.SessionOperationsProvider;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.jpa.impl.NoSQLQueryImpl;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.Type;

/**
 * Delegate most of the work to the underlying Hibernate Session
 * except that queries are redirected to our own engine
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmSessionImpl extends SessionDelegatorBaseImpl implements OgmSession, EventSource {

	private static final Log log = LoggerFactory.make();

	private final EventSource delegate;
	private final OgmSessionFactoryImpl factory;

	private QueryParserService queryParserService;
	private SessionOperations sessionOperations;

	public OgmSessionImpl(OgmSessionFactory factory, EventSource delegate) {
		super( delegate, delegate );
		this.delegate = delegate;
		this.factory = (OgmSessionFactoryImpl) factory;
	}

	//Overridden methods
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public OgmSessionFactory getSessionFactory() {
		return factory;
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Query createQuery(String queryString) throws HibernateException {
		errorIfClosed();
		Map enabledFilters = Collections.EMPTY_MAP; //What here?
		// Use existing Hibernate ORM special-purpose parser to extract the parameters metadata.
		// I think we have the same details in our AST already, but I keep this for now to not
		// diverge too much from ORM code.
		try {
			HQLQueryPlan plan = new HQLQueryPlan( queryString, false, enabledFilters, factory );
			ParameterMetadata parameterMetadata = plan.getParameterMetadata();
			//TODO make sure the HQLQueryPlan et al are cached at some level
			OgmQuery query = new OgmQuery( queryString, getFlushMode(), this, parameterMetadata, getQueryParserService() );
			query.setComment( queryString );
			return query;
		}
		catch ( QuerySyntaxException qse ) {
			throw log.querySyntaxException( qse, queryString );
		}
	}

	@Override
	public Query createQuery(NamedQueryDefinition namedQueryDefinition) {
		String queryString = namedQueryDefinition.getQueryString();
		Query query = createQuery( queryString );
		query.setComment( "named HQL/JP-QL query " + namedQueryDefinition.getName() );
		query.setFlushMode( namedQueryDefinition.getFlushMode() );
		return query;
	}

	private QueryParserService getQueryParserService() {
		if ( queryParserService == null ) {
			queryParserService = getSessionFactory().getServiceRegistry().getService( QueryParserService.class );
		}
		return queryParserService;
	}

	@Override
	public SQLQuery createSQLQuery(String queryString) throws HibernateException {
		errorIfClosed();

		return new NoSQLQueryImpl(
				queryString,
				this,
				factory.getNativeQueryParameterMetadataCache().getParameterMetadata( queryString )
		);
	}

	@Override
	public SQLQuery createSQLQuery(NamedSQLQueryDefinition namedQueryDefinition) {
		errorIfClosed();

		return new NoSQLQueryImpl(
				namedQueryDefinition.getQuery(),
				this,
				factory.getNativeQueryParameterMetadataCache().getParameterMetadata( namedQueryDefinition.getQuery() )
		);
	}

	@Override
	public Query createFilter(Object collection, String queryString) throws HibernateException {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-24", "filters are not supported yet" );
	}

	@Override
	public Filter enableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public Filter getEnabledFilter(String filterName) {
		return delegate.getEnabledFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		throw new IllegalStateException( "Hibernate OGM does not support SQL Connections hence no Work" );
	}

	@Override
	public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
		return delegate.doReturningWork( work );
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	//Event Source methods
	@Override
	public ActionQueue getActionQueue() {
		return delegate.getActionQueue();
	}

	@Override
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException {
		return delegate.instantiate( persister, id );
	}

	@Override
	public void forceFlush(EntityEntry e) throws HibernateException {
		delegate.forceFlush( e );
	}

	@Override
	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException {
		delegate.merge( entityName, object, copiedAlready );
	}

	@Override
	public void persist(String entityName, Object object, Map createdAlready) throws HibernateException {
		delegate.persist( entityName, object, createdAlready );
	}

	@Override
	public void persistOnFlush(String entityName, Object object, Map copiedAlready) {
		delegate.persistOnFlush( entityName, object, copiedAlready );
	}

	@Override
	public void refresh(String entityName, Object object, Map refreshedAlready) throws HibernateException {
		delegate.refresh( entityName, object, refreshedAlready );
	}

	@Override
	public void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, Set transientEntities) {
		delegate.delete( entityName, child, isCascadeDeleteEnabled, transientEntities );
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		return delegate.getJdbcConnectionAccess();
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return delegate.generateEntityKey( id, persister );
	}

	@Override
	public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
		return delegate.generateCacheKey( id, type, entityOrRoleName );
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();

		if ( log.isTraceEnabled() ) {
			log.tracev( "NoSQL query: {0}", customQuery.getSQL() );
		}

		CustomLoader loader = new BackendCustomLoader( (BackendCustomQuery) customQuery, getFactory() );
		return loader.list( getDelegate(), queryParameters );
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.scrollCustomQuery( customQuery, queryParameters );
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		CustomQuery customQuery = new BackendCustomQuery( spec, factory );
		// TODO Implement query plan cache?
		return listCustomQuery( customQuery, queryParameters );
	}

	@Override
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.scroll( spec, queryParameters );
	}

	//SessionImplementor methods
	@Override
	public Query getNamedQuery(String name) {
		errorIfClosed();
		NamedQueryDefinition namedQuery = factory.getNamedQuery( name );
		//ORM looks for native queries when no HQL definition is found, we do the same here.
		if (namedQuery == null) {
			return getNamedSQLQuery( name );
		}

		return createQuery( namedQuery );
	}

	@Override
	public Query getNamedSQLQuery(String queryName) {
		errorIfClosed();
		NamedSQLQueryDefinition nsqlqd = findNamedNativeQuery( queryName );
		Query query = new NoSQLQueryImpl(
				nsqlqd,
				this,
				factory.getNativeQueryParameterMetadataCache().getParameterMetadata( nsqlqd.getQuery() )
		);
		query.setComment( "named native query " + queryName );
		return query;
	}

	private NamedSQLQueryDefinition findNamedNativeQuery(String queryName) {
		NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
		if ( nsqlqd == null ) {
			throw new MappingException( "Named native query not found: " + queryName );
		}
		return nsqlqd;
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new OgmSharedSessionBuilderDelegator( delegate.sessionWithOptions(), factory );
	}

	//Copied from org.hibernate.internal.AbstractSessionImpl.errorIfClosed()
	//to mimic same behaviour
	protected void errorIfClosed() {
		if ( delegate.isClosed() ) {
			throw new SessionException( "Session is closed!" );
		}
	}

	public <G extends GlobalContext<?, ?>, D extends DatastoreConfiguration<G>> G configureDatastore(Class<D> datastoreType) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		delegate.removeOrphanBeforeUpdates( entityName, child );
	}

	/**
	 * Returns the underlying ORM session to which most work is delegated.
	 */
	public EventSource getDelegate() {
		return delegate;
	}

	@Override
	public <P extends SessionOperationsProvider<O>, O extends SessionOperations> O operationsFor(Class<P> datastoreType) {
		if ( sessionOperations == null ) {
			sessionOperations = newInstance( datastoreType ).getSessionOperations( this );
		}

		@SuppressWarnings("unchecked")
		O operations = (O) sessionOperations;
		return operations;
	}

	private <P extends SessionOperationsProvider<?>> P newInstance(Class<P> datastoreType) {
		try {
			return datastoreType.newInstance();

		}
		catch (Exception e) {
			throw log.unableToInstantiateType( datastoreType.getName(), e );
		}
	}
}
