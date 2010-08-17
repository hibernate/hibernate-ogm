package org.hibernate.ogm.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;

/**
 * @author Emmanuel Bernard
 */
public class NoopConnectionProvider implements ConnectionProvider {
	@Override
	public void configure(Properties props) throws HibernateException {
	}

	@Override
	public Connection getConnection() throws SQLException {
		return new NoopConnection();
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
	}

	@Override
	public void close() throws HibernateException {
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return true;
	}
}
