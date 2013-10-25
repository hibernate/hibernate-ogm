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
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Return a JdbcServicesImpl that does not access the underlying database
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmJdbcServicesInitiator extends OptionalServiceInitiator<JdbcServices> {
	public static final OgmJdbcServicesInitiator INSTANCE = new OgmJdbcServicesInitiator();

	@Override
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}

	@Override
	protected JdbcServices buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmJdbcServicesImpl();
	}

	@Override
	protected StandardServiceInitiator<JdbcServices> backupInitiator() {
		return JdbcServicesInitiator.INSTANCE;
	}

	private static final class OgmJdbcServicesImpl implements JdbcServices, ServiceRegistryAwareService, Configurable {
		public JdbcServicesImpl delegate = new JdbcServicesImpl();

		@Override
		public void configure(Map configurationValues) {
			configurationValues.put( "hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE );
			delegate.configure( configurationValues );
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			delegate.injectServices( serviceRegistry );
		}

		@Override
		public ConnectionProvider getConnectionProvider() {
			return delegate.getConnectionProvider();
		}

		@Override
		public Dialect getDialect() {
			return delegate.getDialect();
		}

		@Override
		public SqlStatementLogger getSqlStatementLogger() {
			return delegate.getSqlStatementLogger();
		}

		@Override
		public SqlExceptionHelper getSqlExceptionHelper() {
			return delegate.getSqlExceptionHelper();
		}

		@Override
		public ExtractedDatabaseMetaData getExtractedMetaDataSupport() {
			return delegate.getExtractedMetaDataSupport();
		}

		@Override
		public LobCreator getLobCreator(LobCreationContext lobCreationContext) {
			return delegate.getLobCreator( lobCreationContext );
		}

		@Override
		public ResultSetWrapper getResultSetWrapper() {
			return delegate.getResultSetWrapper();
		}
	}
}
