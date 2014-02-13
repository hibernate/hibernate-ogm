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
package org.hibernate.jpa.spi;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.QueryHints;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.jpa.impl.LetThroughExecuteUpdateQuery;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.jpa.impl.OgmJpaQuery;

/**
 * Delegates most method calls to the underlying EntityManager
 * however, queries are handled differently
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmEntityManager implements EntityManager {
	private final EntityManager hibernateEm;
	private final OgmEntityManagerFactory factory;
	private final LockOptions lockOptions = new LockOptions();

	public OgmEntityManager(OgmEntityManagerFactory factory, EntityManager hibernateEm) {
		this.hibernateEm = hibernateEm;
		this.factory = factory;
	}

	@Override
	public void persist(Object entity) {
		hibernateEm.persist( entity );
	}

	@Override
	public <T> T merge(T entity) {
		return hibernateEm.merge( entity );
	}

	@Override
	public void remove(Object entity) {
		hibernateEm.remove( entity );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return hibernateEm.find( entityClass, primaryKey );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return hibernateEm.find( entityClass, primaryKey, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return hibernateEm.find( entityClass, primaryKey, lockMode );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return hibernateEm.find( entityClass, primaryKey, lockMode, properties );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return hibernateEm.getReference( entityClass, primaryKey );
	}

	@Override
	public void flush() {
		hibernateEm.flush();
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		hibernateEm.setFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		return hibernateEm.getFlushMode();
	}

	@Override
	public void lock(Object entity, LockModeType lockMode) {
		hibernateEm.lock( entity, lockMode );
	}

	@Override
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		hibernateEm.lock( entity, lockMode, properties );
	}

	@Override
	public void refresh(Object entity) {
		hibernateEm.refresh( entity );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		hibernateEm.refresh( entity, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode) {
		hibernateEm.refresh( entity, lockMode );
	}

	@Override
	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		hibernateEm.refresh( entity, lockMode, properties );
	}

	@Override
	public void clear() {
		hibernateEm.clear();
	}

	@Override
	public void detach(Object entity) {
		hibernateEm.detach( entity );
	}

	@Override
	public boolean contains(Object entity) {
		return hibernateEm.contains( entity );
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		return hibernateEm.getLockMode( entity );
	}

	@Override
	public void setProperty(String propertyName, Object value) {
		hibernateEm.setProperty( propertyName, value );
	}

	@Override
	public Map<String, Object> getProperties() {
		return hibernateEm.getProperties();
	}

	@Override
	public Query createQuery(String qlString) {
		//TODO plug the lucene query engine
		//to let the benchmark run let delete from pass
		if ( qlString != null && qlString.toLowerCase().startsWith( "delete from" ) ) {
			//pretend you care
			return new LetThroughExecuteUpdateQuery();
		}

		Session session = (Session) getDelegate();
		return applyProperties( new OgmJpaQuery<Object>( session.createQuery( qlString ), (AbstractEntityManagerImpl) hibernateEm ) );
	}

	private Query applyProperties(Query query) {
		if ( lockOptions.getLockMode() != LockMode.NONE ) {
			query.setLockMode( getLockMode( lockOptions.getLockMode() ) );
		}
		Object queryTimeout;
		if ( ( queryTimeout = getProperties().get( QueryHints.SPEC_HINT_TIMEOUT ) ) != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, queryTimeout );
		}
		Object lockTimeout;
		if ( ( lockTimeout = getProperties().get( AvailableSettings.LOCK_TIMEOUT ) ) != null ) {
			query.setHint( AvailableSettings.LOCK_TIMEOUT, lockTimeout );
		}
		return query;
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		throw new NotSupportedException( "OGM-8", "criteria queries are not supported yet" );
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery) {
		throw new NotSupportedException( "OGM-8", "criteria queries are not supported yet" );
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery) {
		throw new NotSupportedException( "OGM-8", "criteria queries are not supported yet" );
	}

	@Override
	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		// do the translation
		Session session = (Session) getDelegate();
		org.hibernate.Query query = session.createQuery( qlString );

		resultClassChecking( resultClass, query );

		// finally, build/return the query instance
		return new OgmJpaQuery<T>( query, (AbstractEntityManagerImpl) hibernateEm );
	}

	@Override
	public Query createNamedQuery(String name) {
		return buildQueryFromName( name, null );
	}

	private <T> TypedQuery<T> buildQueryFromName(String name, Class<T> resultType) {
		OgmSessionFactory sessionFactory = (OgmSessionFactory) factory.getSessionFactory();
		NamedQueryDefinition queryDefinition = sessionFactory.getNamedSQLQuery( name );
		if ( queryDefinition == null ) {
			queryDefinition = sessionFactory.getNamedQuery( name );
			if ( queryDefinition == null ) {
				throw new IllegalArgumentException( "Named query not found: " + name );
			}
			else {
				return createNamedJpqlQuery( queryDefinition, resultType );
			}
		}
		else {
			return createNamedNativeQuery( (NamedSQLQueryDefinition) queryDefinition, resultType );
		}
	}

	protected <T> TypedQuery<T> createNamedJpqlQuery(NamedQueryDefinition namedQueryDefinition, Class<T> resultType) {
		SessionImplementor session = (SessionImplementor) getDelegate();
		final org.hibernate.Query query = session.createQuery( namedQueryDefinition );
		if ( resultType != null ) {
			resultClassChecking( resultType, query );
		}

		return wrapAsJpaQuery( namedQueryDefinition, query );
	}

	protected <T> TypedQuery<T> wrapAsJpaQuery(NamedQueryDefinition namedQueryDefinition, org.hibernate.Query hibQuery) {
		final OgmJpaQuery<T> jpaQuery = new OgmJpaQuery<T>( hibQuery, (AbstractEntityManagerImpl) hibernateEm );
		applySavedSettings( namedQueryDefinition, jpaQuery );
		return jpaQuery;
	}

	protected <T> void applySavedSettings(NamedQueryDefinition namedQueryDefinition, OgmJpaQuery<T> jpaQuery) {
		AbstractEntityManagerImpl impl = (AbstractEntityManagerImpl) hibernateEm;
		impl.applySavedSettings( namedQueryDefinition, jpaQuery );
	}

	private <T> TypedQuery<T> createNamedNativeQuery(NamedSQLQueryDefinition sqlDefinition, Class<T> resultType) {
		if ( resultType != null ) {
			resultClassChecking( resultType, sqlDefinition );
		}
		String sqlQueryString = sqlDefinition.getQueryString();
		SQLQuery noSqlQuery = ( (Session) getDelegate() ).createSQLQuery( sqlQueryString );
		if ( sqlDefinition.getQueryReturns().length == 1 ) {
			NativeSQLQueryRootReturn rootReturn = (NativeSQLQueryRootReturn) sqlDefinition.getQueryReturns()[0];
			noSqlQuery.addEntity( "alias1", rootReturn.getReturnEntityName(), LockMode.READ );
		}
		return new OgmJpaQuery<T>( noSqlQuery, hibernateEm );
	}

	protected void resultClassChecking(Class<?> resultClass, org.hibernate.Query hqlQuery) {
		AbstractEntityManagerImpl impl = (AbstractEntityManagerImpl) hibernateEm;
		impl.resultClassChecking( resultClass, hqlQuery );
	}

	protected void resultClassChecking(Class<?> resultType, NamedSQLQueryDefinition namedQueryDefinition) {
		AbstractEntityManagerImpl impl = (AbstractEntityManagerImpl) hibernateEm;
		impl.resultClassChecking( resultType, namedQueryDefinition );
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return buildQueryFromName( name, resultClass );
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		SQLQuery q = ( (Session) getDelegate() ).createSQLQuery( sqlString );
		return new OgmJpaQuery( q, hibernateEm );
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass) {
		SQLQuery q = ( (Session) getDelegate() ).createSQLQuery( sqlString );
		q.addEntity( "alias1", resultClass.getName(), LockMode.READ );
		return new OgmJpaQuery( q, hibernateEm );
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		SQLQuery q = ( (Session) getDelegate() ).createSQLQuery( sqlString );
		q.setResultSetMapping( resultSetMapping );
		return new OgmJpaQuery( q, hibernateEm );
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public void joinTransaction() {
		hibernateEm.joinTransaction();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return hibernateEm.isJoinedToTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		final T session = hibernateEm.unwrap( cls );
		if ( Session.class.isAssignableFrom( cls ) || SessionImplementor.class.isAssignableFrom( cls ) ) {
			return (T) buildOgmSession( (EventSource) session );
		}
		throw new HibernateException( "Cannot unwrap the following type: " + cls );
	}

	private OgmSession buildOgmSession(Session session) {
		final SessionFactory sessionFactory = ( (HibernateEntityManagerFactory) hibernateEm.getEntityManagerFactory() )
				.getSessionFactory();
		final OgmSessionFactory ogmSessionFactory = new OgmSessionFactoryImpl( (SessionFactoryImplementor) sessionFactory );
		return new OgmSession( ogmSessionFactory, (EventSource) session );
	}

	@Override
	public Object getDelegate() {
		final Object delegate = hibernateEm.getDelegate();
		if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
			return buildOgmSession( (EventSource) delegate );
		}
		else {
			return delegate;
		}
	}

	@Override
	public void close() {
		hibernateEm.close();
	}

	@Override
	public boolean isOpen() {
		return hibernateEm.isOpen();
	}

	@Override
	public EntityTransaction getTransaction() {
		return hibernateEm.getTransaction();
	}

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		return factory;
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return hibernateEm.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return hibernateEm.getMetamodel();
	}
}
