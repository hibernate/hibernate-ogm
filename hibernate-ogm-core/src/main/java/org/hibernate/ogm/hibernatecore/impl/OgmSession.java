/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Interceptor;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

/**
 * Delegate most of the work to the underlying Hibernate Session
 * except that queries are redirected to our own engine
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmSession implements org.hibernate.Session, EventSource {

	private static final Log log = LoggerFactory.make();

	private final EventSource delegate;
	private final OgmSessionFactory factory;
	private QueryParserService queryParserService;

	public OgmSession(OgmSessionFactory factory, EventSource delegate) {
		this.delegate = delegate;
		this.factory = factory;
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

	private QueryParserService getQueryParserService() {
		if ( queryParserService == null ) {
			queryParserService = getSessionFactory().getServiceRegistry().getService( QueryParserService.class );
		}
		return queryParserService;
	}

	@Override
	public SQLQuery createSQLQuery(String queryString) throws HibernateException {
		throw new IllegalStateException( "Hibernate OGM does not support native queries" );
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
	public void refresh(Object object, Map refreshedAlready) throws HibernateException {
		delegate.refresh( object, refreshedAlready );
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

	//SessionImplementor methods
	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public void setAutoClear(boolean enabled) {
		delegate.setAutoClear( enabled );
	}

	@Override
	public void disableTransactionAutoJoin() {
		delegate.disableTransactionAutoJoin();
	}

	@Override
	public boolean isTransactionInProgress() {
		return delegate.isTransactionInProgress();
	}

	@Override
	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
		delegate.initializeCollection( collection, writing );
	}

	@Override
	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
			throws HibernateException {
		return delegate.internalLoad( entityName, id, eager, nullable );
	}

	@Override
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		return delegate.immediateLoad( entityName, id );
	}

	@Override
	public long getTimestamp() {
		return delegate.getTimestamp();
	}

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.list( query, queryParameters );
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.iterate( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.scroll( query, queryParameters );
	}

	@Override
	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
		return delegate.scroll( criteria, scrollMode );
	}

	@Override
	public List list(CriteriaImpl criteria) {
		return delegate.list( criteria );
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.listFilter( collection, filter, queryParameters );
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.iterateFilter( collection, filter, queryParameters );
	}

	@Override
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
		return delegate.getEntityPersister( entityName, object );
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		return delegate.getEntityUsingInterceptor( key );
	}

	@Override
	public Serializable getContextEntityIdentifier(Object object) {
		return delegate.getContextEntityIdentifier( object );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		return delegate.bestGuessEntityName( object );
	}

	@Override
	public String guessEntityName(Object entity) throws HibernateException {
		return delegate.guessEntityName( entity );
	}

	@Override
	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return delegate.instantiate( entityName, id );
	}

	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		return delegate.listCustomQuery( customQuery, queryParameters );
	}

	@Override
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.scrollCustomQuery( customQuery, queryParameters );
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return delegate.list( spec, queryParameters );
	}

	@Override
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.scroll( spec, queryParameters );
	}

	@Override
	public Object getFilterParameterValue(String filterParameterName) {
		return delegate.getFilterParameterValue( filterParameterName );
	}

	@Override
	public Type getFilterParameterType(String filterParameterName) {
		return delegate.getFilterParameterType( filterParameterName );
	}

	@Override
	public Map getEnabledFilters() {
		return delegate.getEnabledFilters();
	}

	@Override
	public int getDontFlushFromFind() {
		return delegate.getDontFlushFromFind();
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		return delegate.getPersistenceContext();
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		return delegate.executeUpdate( query, queryParameters );
	}

	@Override
	public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters)
			throws HibernateException {
		return delegate.executeNativeUpdate( specification, queryParameters );
	}

	@Override
	public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
		return delegate.getNonFlushedChanges();
	}

	@Override
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
		delegate.applyNonFlushedChanges( nonFlushedChanges );
	}

	//SessionImplementor methods
	@Override
	public Query getNamedQuery(String name) {
		return delegate.getNamedQuery( name );
	}

	@Override
	public Query getNamedSQLQuery(String name) {
		return delegate.getNamedSQLQuery( name );
	}

	@Override
	public boolean isEventSource() {
		return delegate.isEventSource();
	}

	@Override
	public void afterScrollOperation() {
		delegate.afterScrollOperation();
	}

	@Override
	public String getFetchProfile() {
		return delegate.getFetchProfile();
	}

	@Override
	public void setFetchProfile(String name) {
		delegate.setFetchProfile( name );
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return delegate.getTransactionCoordinator();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return delegate.getLoadQueryInfluencers();
	}

	//Session methods
	@Override
	public CacheMode getCacheMode() {
		return delegate.getCacheMode();
	}

	@Override
	public void setCacheMode(CacheMode cm) {
		delegate.setCacheMode( cm );
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public FlushMode getFlushMode() {
		return delegate.getFlushMode();
	}

	@Override
	public void setFlushMode(FlushMode fm) {
		delegate.setFlushMode( fm );
	}

	@Override
	public Connection connection() {
		return delegate.connection();
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new OgmSharedSessionBuilderDelegator( delegate.sessionWithOptions(), factory );
	}

	@Override
	public void flush() {
		delegate.flush();
	}

	@Override
	public Connection close() throws HibernateException {
		return delegate.close();
	}

	@Override
	public void cancelQuery() throws HibernateException {
		delegate.cancelQuery();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		return delegate.isDirty();
	}

	@Override
	public boolean isDefaultReadOnly() {
		return delegate.isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean readOnly) {
		delegate.setDefaultReadOnly( readOnly );
	}

	@Override
	public Serializable getIdentifier(Object object) throws HibernateException {
		return delegate.getIdentifier( object );
	}

	@Override
	public boolean contains(Object object) {
		return delegate.contains( object );
	}

	@Override
	public void evict(Object object) throws HibernateException {
		delegate.evict( object );
	}

	@Override
	public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
		return delegate.load( theClass, id, lockMode );
	}

	@Override
	public Object load(Class theClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return delegate.load( theClass, id, lockOptions );
	}

	@Override
	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return delegate.load( entityName, id, lockMode );
	}

	@Override
	public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return delegate.load( entityName, id, lockOptions );
	}

	@Override
	public Object load(Class theClass, Serializable id) throws HibernateException {
		return delegate.load( theClass, id );
	}

	@Override
	public Object load(String entityName, Serializable id) throws HibernateException {
		return delegate.load( entityName, id );
	}

	@Override
	public void load(Object object, Serializable id) throws HibernateException {
		delegate.load( object, id );
	}

	@Override
	public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {
		delegate.replicate( object, replicationMode );
	}

	@Override
	public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {
		delegate.replicate( entityName, object, replicationMode );
	}

	@Override
	public Serializable save(Object object) throws HibernateException {
		return delegate.save( object );
	}

	@Override
	public Serializable save(String entityName, Object object) throws HibernateException {
		return delegate.save( entityName, object );
	}

	@Override
	public void saveOrUpdate(Object object) throws HibernateException {
		delegate.saveOrUpdate( object );
	}

	@Override
	public void saveOrUpdate(String entityName, Object object) throws HibernateException {
		delegate.saveOrUpdate( entityName, object );
	}

	@Override
	public void update(Object object) throws HibernateException {
		delegate.update( object );
	}

	@Override
	public void update(String entityName, Object object) throws HibernateException {
		delegate.update( entityName, object );
	}

	@Override
	public Object merge(Object object) throws HibernateException {
		return delegate.merge( object );
	}

	@Override
	public Object merge(String entityName, Object object) throws HibernateException {
		return delegate.merge( entityName, object );
	}

	@Override
	public void persist(Object object) throws HibernateException {
		delegate.persist( object );
	}

	@Override
	public void persist(String entityName, Object object) throws HibernateException {
		delegate.persist( entityName, object );
	}

	@Override
	public void delete(Object object) throws HibernateException {
		delegate.delete( object );
	}

	@Override
	public void delete(String entityName, Object object) throws HibernateException {
		delegate.delete( entityName, object );
	}

	@Override
	public void lock(Object object, LockMode lockMode) throws HibernateException {
		delegate.lock( object, lockMode );
	}

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		delegate.lock( entityName, object, lockMode );
	}

	@Override
	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return delegate.buildLockRequest( lockOptions );
	}

	@Override
	public void refresh(Object object) throws HibernateException {
		delegate.refresh( object );
	}

	@Override
	public void refresh(String entityName, Object object) throws HibernateException {
		delegate.refresh( entityName, object );
	}

	@Override
	public void refresh(Object object, LockMode lockMode) throws HibernateException {
		delegate.refresh( object, lockMode );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
		delegate.refresh( object, lockOptions );
	}

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) throws HibernateException {
		delegate.refresh( entityName, object, lockOptions);
	}

	@Override
	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		return delegate.getCurrentLockMode( object );
	}

	@Override
	public String getTenantIdentifier() {
		return delegate.getTenantIdentifier();
	}

	@Override
	public Transaction beginTransaction() throws HibernateException {
		return delegate.beginTransaction();
	}

	@Override
	public Transaction getTransaction() {
		return delegate.getTransaction();
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Object get(Class clazz, Serializable id) throws HibernateException {
		return delegate.get( clazz, id );
	}

	@Override
	public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
		return delegate.get( clazz, id, lockMode );
	}

	@Override
	public Object get(Class clazz, Serializable id, LockOptions lockOptions) throws HibernateException {
		return delegate.get( clazz, id, lockOptions );
	}

	@Override
	public Object get(String entityName, Serializable id) throws HibernateException {
		return delegate.get( entityName, id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return delegate.get( entityName, id, lockMode );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return delegate.get( entityName, id, lockOptions );
	}

	@Override
	public String getEntityName(Object object) throws HibernateException {
		return delegate.getEntityName( object );
	}

	@Override
	public SessionStatistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		return delegate.isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entityOrProxy, boolean readOnly) {
		delegate.setReadOnly( entityOrProxy, readOnly );
	}

	@Override
	public Connection disconnect() throws HibernateException {
		return delegate.disconnect();
	}

	@Override
	public void reconnect(Connection connection) throws HibernateException {
		delegate.reconnect( connection );
	}

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return delegate.isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		delegate.enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		delegate.disableFetchProfile( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return delegate.getTypeHelper();
	}

	@Override
	public LobHelper getLobHelper() {
		return delegate.getLobHelper();
	}

	@Override
	public <T> T execute(Callback<T> callback) {
		return delegate.execute( callback );
	}

	@Override
	public IdentifierLoadAccess byId(Class entityClass) {
		return delegate.byId( entityClass );
	}

	@Override
	public IdentifierLoadAccess byId(String entityName) {
		return delegate.byId( entityName );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return delegate.byNaturalId( entityName );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		return delegate.byNaturalId( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return delegate.bySimpleNaturalId( entityName );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		return delegate.bySimpleNaturalId( entityClass );
	}

	//Copied from org.hibernate.internal.AbstractSessionImpl.errorIfClosed()
	//to mimic same behaviour
	protected void errorIfClosed() {
		if ( delegate.isClosed() ) {
			throw new SessionException( "Session is closed!" );
		}
	}

}

