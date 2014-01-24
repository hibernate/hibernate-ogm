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
package org.hibernate.ogm.test.batch;

import org.fest.assertions.Assertions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.datastore.map.impl.HashMapDialect;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.test.simpleentity.Helicopter;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 *
 */
public class BatchInsertTest extends OgmTestCase {

	@Test
	public void testBatchExecutionWhenAutoFlushed() throws Exception {
		final Session session = openSession();
		session.beginTransaction();
		int numInsert = 6;
		for ( int i = 0; i < numInsert; i++ ) {
			session.persist( helicopter( "H" + i ) );
		}
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( SampleBatchableDialect.queueSize ).isEqualTo( numInsert );
	}

	private Helicopter helicopter(String name) {
		Helicopter helicopter = new Helicopter();
		helicopter.setName( name );
		return helicopter;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( DatastoreProviderInitiator.DATASTORE_PROVIDER, AvailableDatastoreProvider.MAP.getDatastoreProviderClassName() );
		cfg.setProperty( OgmConfiguration.OGM_GRID_DIALECT, SampleBatchableDialect.class.getName() );
	}

	public static class SampleBatchableDialect extends HashMapDialect implements BatchableGridDialect {

		static volatile int queueSize = 0;

		public SampleBatchableDialect(MapDatastoreProvider provider) {
			super( provider );
		}

		@Override
		public void executeBatch(OperationsQueue queue) {
			while ( queue.poll() != null ) {
				queueSize++;
			}
		}

	}

}
