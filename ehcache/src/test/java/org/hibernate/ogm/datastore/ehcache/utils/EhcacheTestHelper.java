/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.ehcache.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.ehcache.Ehcache;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableKey;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.test.utils.TestableGridDialect;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getProvider( sessionFactory ).getEntityCache().getSize();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return getProvider( sessionFactory ).getAssociationCache().getSize();
	}

	@Override
	public Map<String,Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		@SuppressWarnings("unchecked")
		Map<String, Object> tuple = (Map<String, Object>) getProvider( sessionFactory )
				.getEntityCache()
				.get( new SerializableKey( key ) )
				.getObjectValue();

		return tuple;
	}

	private static EhcacheDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry()
				.getService( DatastoreProvider.class );
		if ( !( EhcacheDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Ehcache, cannot extract underlying cache" );
		}
		return EhcacheDatastoreProvider.class.cast( provider );
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

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public long getNumberOEmbeddedCollections(SessionFactory sessionFactory) {
		throw new UnsupportedOperationException( "This datastore does not support storing collections embedded within entities." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Ehcache.class );
	}
}
