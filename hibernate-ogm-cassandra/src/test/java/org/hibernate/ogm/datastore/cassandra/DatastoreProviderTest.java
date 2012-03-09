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
package org.hibernate.ogm.datastore.cassandra;

import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreProviderTest {

	@Test( expected = IllegalArgumentException.class )
	public void getConfigurationShouldThrowExceptionOnInvalidSetting() {
		CassandraDatastoreProvider provider = new CassandraDatastoreProvider();
		provider.configure( null );
	}

	@Test
	public void defaultUrlValueShouldSuccessOnConfiguration() {
		CassandraDatastoreProvider provider = new CassandraDatastoreProvider();

		Map configuration = new HashMap();
		configuration.put( CassandraDatastoreProvider.CASSANDRA_KEYSPACE, "keyspace1" );
		provider.configure( configuration );
	}

	@Test
	public void startAndStopShouldSuccessOnRightConfiguration() {
		CassandraDatastoreProvider provider = new CassandraDatastoreProvider();

		Map configuration = new HashMap();
		configuration.put( CassandraDatastoreProvider.CASSANDRA_KEYSPACE, "keyspace1" );
		configuration.put( CassandraDatastoreProvider.CASSANDRA_URL, "jdbc:cassandra://localhost:9160" );
		provider.configure( configuration );
		provider.start();
		provider.stop();
	}


	@Test
	public void startAndStopShouldCreateKeyspaces() {
		CassandraDatastoreProvider provider = new CassandraDatastoreProvider();

		Map configuration = new HashMap();
		configuration.put( CassandraDatastoreProvider.CASSANDRA_KEYSPACE, "keyspace1" );
		configuration.put( CassandraDatastoreProvider.CASSANDRA_URL, "jdbc:cassandra://localhost:9160" );
		configuration.put( CassandraDatastoreProvider.CASSANDRA_HBM2DDL_AUTO, "create-drop" );
		provider.configure( configuration );
		provider.start();
		provider.stop();
	}
}
