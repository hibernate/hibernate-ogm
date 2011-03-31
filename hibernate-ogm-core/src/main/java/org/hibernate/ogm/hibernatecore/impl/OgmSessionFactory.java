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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;

import org.hibernate.Cache;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.TypeHelper;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.Region;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.event.EventSource;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmSessionFactory implements SessionFactoryImplementor {
	private final SessionFactoryImplementor delegate;

	public OgmSessionFactory(SessionFactoryImplementor delegate) {
		this.delegate = delegate;
	}

	@Override
	public TypeResolver getTypeResolver() {
		return delegate.getTypeResolver();
	}

	@Override
	public Properties getProperties() {
		return delegate.getProperties();
	}

	@Override
	public EntityPersister getEntityPersister(String entityName) throws MappingException {
		return delegate.getEntityPersister( entityName );
	}

	@Override
	public CollectionPersister getCollectionPersister(String role) throws MappingException {
		return delegate.getCollectionPersister( role );
	}

	@Override
	public Dialect getDialect() {
		return delegate.getDialect();
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public QueryPlanCache getQueryPlanCache() {
		return delegate.getQueryPlanCache();
	}

	@Override
	public Type[] getReturnTypes(String queryString) throws HibernateException {
		return delegate.getReturnTypes( queryString );
	}

	@Override
	public String[] getReturnAliases(String queryString) throws HibernateException {
		return delegate.getReturnAliases( queryString );
	}

	@Override
	public ConnectionProvider getConnectionProvider() {
		return delegate.getConnectionProvider();
	}

	@Override
	public String[] getImplementors(String className) throws MappingException {
		return delegate.getImplementors( className );
	}

	@Override
	public String getImportedClassName(String name) {
		return delegate.getImportedClassName( name );
	}

	@Override
	public TransactionManager getTransactionManager() {
		return delegate.getTransactionManager();
	}

	@Override
	public QueryCache getQueryCache() {
		return delegate.getQueryCache();
	}

	@Override
	public QueryCache getQueryCache(String regionName) throws HibernateException {
		return delegate.getQueryCache( regionName );
	}

	@Override
	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return delegate.getUpdateTimestampsCache();
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return delegate.getStatisticsImplementor();
	}

	@Override
	public NamedQueryDefinition getNamedQuery(String queryName) {
		return delegate.getNamedQuery( queryName );
	}

	@Override
	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return delegate.getNamedSQLQuery( queryName );
	}

	@Override
	public ResultSetMappingDefinition getResultSetMapping(String name) {
		return delegate.getResultSetMapping( name );
	}

	@Override
	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return delegate.getIdentifierGenerator( rootEntityName );
	}

	@Override
	public Region getSecondLevelCacheRegion(String regionName) {
		return delegate.getSecondLevelCacheRegion( regionName );
	}

	@Override
	public Map getAllSecondLevelCacheRegions() {
		return delegate.getAllSecondLevelCacheRegions();
	}

	@Override
	public SQLExceptionConverter getSQLExceptionConverter() {
		return delegate.getSQLExceptionConverter();
	}

	@Override
	public Settings getSettings() {
		return delegate.getSettings();
	}

	@Override
	public Session openTemporarySession() throws HibernateException {
		return new OgmSession( this, (EventSource) delegate.openTemporarySession() );
	}

	@Override
	public Session openSession(Connection connection, boolean flushBeforeCompletionEnabled, boolean autoCloseSessionEnabled, ConnectionReleaseMode connectionReleaseMode)
			throws HibernateException {
		final Session session = delegate.openSession(
				connection, flushBeforeCompletionEnabled, autoCloseSessionEnabled, connectionReleaseMode
		);
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return delegate.getCollectionRolesByEntityParticipant( entityName );
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return delegate.getEntityNotFoundDelegate();
	}

	@Override
	public SQLFunctionRegistry getSqlFunctionRegistry() {
		return delegate.getSqlFunctionRegistry();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return delegate.getFetchProfile( name );
	}

	@Override
	public SessionFactoryObserver getFactoryObserver() {
		return delegate.getFactoryObserver();
	}

	@Override
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return delegate.getIdentifierGeneratorFactory();
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return delegate.getIdentifierType( className );
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return delegate.getIdentifierPropertyName( className );
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return delegate.getReferencedPropertyType( className, propertyName );
	}

	@Override
	public Session openSession() throws HibernateException {
		final Session session = delegate.openSession();
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public Session openSession(Interceptor interceptor) throws HibernateException {
		final Session session = delegate.openSession( interceptor );
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public Session openSession(Connection connection) {
		final Session session = delegate.openSession( connection );
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public Session openSession(Connection connection, Interceptor interceptor) {
		final Session session = delegate.openSession( connection, interceptor );
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public Session getCurrentSession() throws HibernateException {
		final Session session = delegate.getCurrentSession();
		return new OgmSession(this, (EventSource) session);
	}

	@Override
	public StatelessSession openStatelessSession() {
		throw new NotSupportedException( "OGM-18", "Stateless session is not implemented in OGM" );
	}

	@Override
	public StatelessSession openStatelessSession(Connection connection) {
		throw new NotSupportedException( "OGM-18", "Stateless session is not implemented in OGM" );
	}

	@Override
	public ClassMetadata getClassMetadata(Class entityClass) {
		return delegate.getClassMetadata( entityClass );
	}

	@Override
	public ClassMetadata getClassMetadata(String entityName) {
		return delegate.getClassMetadata( entityName );
	}

	@Override
	public CollectionMetadata getCollectionMetadata(String roleName) {
		return delegate.getCollectionMetadata( roleName );
	}

	@Override
	public Map<String, ClassMetadata> getAllClassMetadata() {
		return delegate.getAllClassMetadata();
	}

	@Override
	public Map getAllCollectionMetadata() {
		return delegate.getAllCollectionMetadata();
	}

	@Override
	public Statistics getStatistics() {
		return delegate.getStatistics();
	}

	@Override
	public void close() throws HibernateException {
		delegate.close();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public Cache getCache() {
		return delegate.getCache();
	}

	@Override
	public void evict(Class persistentClass) throws HibernateException {
		delegate.evict( persistentClass );
	}

	@Override
	public void evict(Class persistentClass, Serializable id) throws HibernateException {
		delegate.evict( persistentClass, id );
	}

	@Override
	public void evictEntity(String entityName) throws HibernateException {
		delegate.evictEntity( entityName );
	}

	@Override
	public void evictEntity(String entityName, Serializable id) throws HibernateException {
		delegate.evictEntity( entityName, id );
	}

	@Override
	public void evictCollection(String roleName) throws HibernateException {
		delegate.evictCollection( roleName );
	}

	@Override
	public void evictCollection(String roleName, Serializable id) throws HibernateException {
		delegate.evictCollection( roleName, id );
	}

	@Override
	public void evictQueries(String cacheRegion) throws HibernateException {
		delegate.evictQueries( cacheRegion );
	}

	@Override
	public void evictQueries() throws HibernateException {
		delegate.evictQueries();
	}

	@Override
	public Set getDefinedFilterNames() {
		return delegate.getDefinedFilterNames();
	}

	@Override
	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		return delegate.getFilterDefinition( filterName );
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return delegate.containsFetchProfileDefinition( name );
	}

	@Override
	public TypeHelper getTypeHelper() {
		return delegate.getTypeHelper();
	}

	@Override
	public Reference getReference() throws NamingException {
		throw new NotSupportedException( "OGM-19", "OGM SessionFactory cannot be referenced" );
	}
}
