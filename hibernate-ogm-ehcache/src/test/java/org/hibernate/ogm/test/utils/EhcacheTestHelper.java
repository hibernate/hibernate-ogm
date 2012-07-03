/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.utils;

import java.util.Map;

import net.sf.ehcache.Cache;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {
	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		return getEntityCache( sessionFactory ).getSize() == numberOfEntities;
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		return getAssociationCache( sessionFactory ).getSize() == numberOfAssociations;
	}

	@Override
	public Map<String,Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return (Map) getEntityCache( sessionFactory ).get( key ).getValue();
	}

	private static Cache getEntityCache(SessionFactory sessionFactory) {
		EhcacheDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getCache( ENTITY_STORE );
	}

	private static EhcacheDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry()
				.getService( DatastoreProvider.class );
		if ( !( EhcacheDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Ehcache, cannot extract underlying cache" );
		}
		return EhcacheDatastoreProvider.class.cast( provider );
	}

	private static Cache getAssociationCache(SessionFactory sessionFactory) {
		EhcacheDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getCache( ASSOCIATION_STORE );
	}

	/**
	 * TODO - EHCache _is_ transactional. Turn this on. We could turn on XA or Local.
	 * Local will be faster. We will pick this up from the cache config.
	 *
	 * @return
	 */
	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		//Nothing to do
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}
}
