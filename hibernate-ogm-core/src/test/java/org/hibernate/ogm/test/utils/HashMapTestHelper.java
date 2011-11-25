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

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mapbased.impl.MapBasedDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class HashMapTestHelper implements TestableGridDialect {

	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		return getEntityMap( sessionFactory ).size();
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		 return getAssociationCache( sessionFactory ).size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return getEntityMap( sessionFactory ).get( key );
	}

	private static Map<EntityKey,Map<String, Object>> getEntityMap(SessionFactory sessionFactory) {
		MapBasedDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getEntityMap();
	}

	private static MapBasedDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( MapBasedDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException("Not testing with MapBasedDatastoreProvider, cannot extract underlying map");
		}
		return MapBasedDatastoreProvider.class.cast(provider);
	}

	private static Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache(SessionFactory sessionFactory) {
		MapBasedDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getAssociationsMap();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

}
