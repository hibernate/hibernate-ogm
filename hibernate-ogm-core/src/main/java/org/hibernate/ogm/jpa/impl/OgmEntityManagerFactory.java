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
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;

/**
 * Delegate most work to the underlying EntityManagerFactory.
 * REturn an OgmEntityManager to cope with query operations
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmEntityManagerFactory implements EntityManagerFactory, HibernateEntityManagerFactory {
	private final EntityManagerFactory hibernateEmf;

	public OgmEntityManagerFactory(EntityManagerFactory hibernateEmf) {
		this.hibernateEmf = hibernateEmf;
	}

	@Override
	public EntityManager createEntityManager() {
		return new OgmEntityManager( this, hibernateEmf.createEntityManager() );
	}

	@Override
	public EntityManager createEntityManager(Map map) {
		return new OgmEntityManager( this, hibernateEmf.createEntityManager(map) );
	}

	@Override
	public SessionFactory getSessionFactory() {
		final SessionFactory sessionFactory = ( ( HibernateEntityManagerFactory ) hibernateEmf ).getSessionFactory();
		return new OgmSessionFactory( ( SessionFactoryImplementor ) sessionFactory );
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
}
