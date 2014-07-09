/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.jpa;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
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
