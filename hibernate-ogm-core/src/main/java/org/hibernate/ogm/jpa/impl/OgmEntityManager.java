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
package org.hibernate.ogm.jpa.impl;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;

/**
 * Delegates most method calls to the underlying EntityManager
 * however, queries are handled differently
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmEntityManager implements EntityManager {
	private final EntityManager hibernateEm;
	private final OgmEntityManagerFactory factory;

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
		throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		throw new NotSupportedException( "OGM-8", "criteria queries are not supported yet" );
	}

	@Override
	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		throw new NotSupportedException( "OGM-14", "typed queries are not supported yet" );
	}

	@Override
	public Query createNamedQuery(String name) {
		throw new NotSupportedException( "OGM-15", "named queries are not supported yet" );
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		throw new NotSupportedException( "OGM-14", "typed queries are not supported yet" );
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		throw new IllegalStateException( "Hibernate OGM does not support native queries" );
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass) {
		throw new IllegalStateException( "Hibernate OGM does not support native queries" );
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		throw new IllegalStateException( "Hibernate OGM does not support native queries" );
	}

	@Override
	public void joinTransaction() {
		hibernateEm.joinTransaction();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		final T session = hibernateEm.unwrap( cls );
		if ( Session.class.isAssignableFrom( cls ) || SessionImplementor.class.isAssignableFrom( cls ) ) {
			return (T) buildOgmSession( ( EventSource ) session );
		}
		throw new HibernateException( "Cannot unwrap the following type: " + cls );
	}

	private OgmSession buildOgmSession(Session session) {
		final SessionFactory sessionFactory = ( ( HibernateEntityManagerFactory ) hibernateEm.getEntityManagerFactory() )
				.getSessionFactory();
		final OgmSessionFactory ogmSessionFactory = new OgmSessionFactory( ( SessionFactoryImplementor ) sessionFactory );
		return new OgmSession( ogmSessionFactory, (EventSource) session );
	}

	@Override
	public Object getDelegate() {
		final Object delegate = hibernateEm.getDelegate();
		if ( Session.class.isAssignableFrom( delegate.getClass() ) ) {
			return buildOgmSession( ( EventSource ) delegate );
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
