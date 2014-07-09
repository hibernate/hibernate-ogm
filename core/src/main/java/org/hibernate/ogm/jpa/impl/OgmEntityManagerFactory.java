/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.internal.metamodel.EntityTypeImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;

/**
 * Delegate most work to the underlying EntityManagerFactory.
 * REturn an OgmEntityManager to cope with query operations
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmEntityManagerFactory implements EntityManagerFactory, HibernateEntityManagerFactory {
	private final EntityManagerFactory hibernateEmf;

	public OgmEntityManagerFactory(EntityManagerFactory hibernateEmf) {
		this.hibernateEmf = hibernateEmf;
	}

	@Override
	public EntityManager createEntityManager() {
		return new OgmEntityManager( this, (HibernateEntityManagerImplementor) hibernateEmf.createEntityManager() );
	}

	@Override
	public EntityManager createEntityManager(Map map) {
		return new OgmEntityManager( this, (HibernateEntityManagerImplementor) hibernateEmf.createEntityManager( map ) );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		return new OgmEntityManager( this, (HibernateEntityManagerImplementor) hibernateEmf.createEntityManager( synchronizationType ) );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
		return new OgmEntityManager( this, (HibernateEntityManagerImplementor) hibernateEmf.createEntityManager( synchronizationType, map ) );
	}

	@Override
	public SessionFactory getSessionFactory() {
		final SessionFactory sessionFactory = ( (HibernateEntityManagerFactory) hibernateEmf ).getSessionFactory();
		return new OgmSessionFactoryImpl( (SessionFactoryImplementor) sessionFactory );
	}

	@Override
	public void addNamedQuery(String name, Query query) {
		hibernateEmf.addNamedQuery( name, query );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	//Delegation

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return hibernateEmf.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return hibernateEmf.getMetamodel();
	}

	@Override
	public boolean isOpen() {
		return hibernateEmf.isOpen();
	}

	@Override
	public void close() {
		hibernateEmf.close();
	}

	@Override
	public Map<String, Object> getProperties() {
		return hibernateEmf.getProperties();
	}

	@Override
	public Cache getCache() {
		return hibernateEmf.getCache();
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return hibernateEmf.getPersistenceUnitUtil();
	}

	@Override
	public EntityTypeImpl<?> getEntityTypeByName(String entityName) {
		return ( (HibernateEntityManagerFactory) hibernateEmf ).getEntityTypeByName( entityName );
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		if ( cls != null && cls.isAssignableFrom( getClass() ) ) {
			@SuppressWarnings("unchecked")
			T result = (T) this;
			return result;
		}

		return hibernateEmf.unwrap( cls );
	}
}
