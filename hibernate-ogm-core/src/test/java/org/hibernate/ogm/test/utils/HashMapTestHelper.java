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
package org.hibernate.ogm.test.utils;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.impl.mapbased.MapBasedDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HashMapTestHelper implements DataStoreSpecificTestHelper {

	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		return getEntityMap( sessionFactory ).size();
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		 return getAssociationCache( sessionFactory ).size();
	}

	@Override
	public Map extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return (Map) getEntityMap( sessionFactory ).get( key );
	}

	private static Map getEntityMap(SessionFactory sessionFactory) {
		MapBasedDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getEntityMap();
	}

	private static MapBasedDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( ! (MapBasedDatastoreProvider.class.isInstance(provider) ) ) {
			throw new RuntimeException("Not testing with MapBasedDatastoreProvider, cannot extract underlying map");
		}
		return MapBasedDatastoreProvider.class.cast(provider);
	}

	private static Map getAssociationCache(SessionFactory sessionFactory) {
		MapBasedDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getAssociationsMap();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

}
