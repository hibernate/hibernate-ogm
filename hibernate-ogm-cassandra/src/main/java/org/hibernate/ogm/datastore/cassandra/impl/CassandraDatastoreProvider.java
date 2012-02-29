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
package org.hibernate.ogm.datastore.cassandra.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//FIXME use a connection pool for Cassandra's connections
public class CassandraDatastoreProvider implements DatastoreProvider, Startable, Stoppable,
		ServiceRegistryAwareService, Configurable {
	private Connection connection;
	private String url = "jdbc:cassandra://localhost:9160";
	private String keyspace;

	public CassandraDatastoreProvider() {
		ClassLoaderHelper.classForName("org.apache.cassandra.cql.jdbc.CassandraDriver", CassandraDatastoreProvider.class, "Cassandra Driver");
	}
	@Override
	public void configure(Map configurationValues) {
		keyspace = "Keyspace1";
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return null;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void start() {
		try {
			connection = DriverManager.getConnection(url);

		}
		catch (Exception e) {
			throw new HibernateException("Unable to connect to Cassandra server " + url, e);
		}

		//in dev mode, use simple keyspace generation
		//FIXME disable for prod??
		try {
			StringBuilder statement = new StringBuilder()
					.append("CREATE KEYSPACE ")
					.append(keyspace)
					.append(" WITH strategy_class = 'SimpleStrategy'")
					.append(" AND strategy_options:replication_factor = 1;");
			connection.createStatement().execute(statement.toString());
		}
		catch (SQLSyntaxErrorException e) {
			if (!e.getMessage().startsWith("Keyspace names must be case-insensitively unique")) {
				throw new HibernateException("Unable create the keyspace " + keyspace, e);
			}
			//else the keyspace is already created
		}
		catch (Exception e) {
			throw new HibernateException("Unable create the keyspace " + keyspace, e);
		}
	}

	@Override
	public void stop() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				//FIXME log a warning
			}
		}
	}
}
