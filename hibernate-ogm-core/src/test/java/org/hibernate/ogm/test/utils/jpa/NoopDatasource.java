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
package org.hibernate.ogm.test.utils.jpa;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class NoopDatasource implements DataSource {
	@Override
	public Connection getConnection() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
