/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.test.utils.TestableGridDialect;

/**
 * @author Khanh Tuong Maudoux
 */
public class CassandraTestHelper implements TestableGridDialect {

	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		throw new NotSupportedException("OGM-122", "use a cache");
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		throw new NotSupportedException("OGM-122", "use a cache");
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		throw new NotSupportedException("OGM-122", "use a cache");
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		CassandraDatastoreProvider provider = getProvider( sessionFactory );
		provider.stop();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	public static CassandraDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry().getService(DatastoreProvider.class);
		if ( ! (CassandraDatastoreProvider.class.isInstance(provider) ) ) {
			throw new RuntimeException("Not testing with Cassandra");
		}
		return CassandraDatastoreProvider.class.cast(provider);
	}

}
