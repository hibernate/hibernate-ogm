/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import org.hibernate.datastore.ogm.orientdb.OrientDBDialect;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.spi.PropertyReaderContext;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static Log LOG = LoggerFactory.getLogger();
	private ConfigurationPropertyReader propertyReader;
	private ServiceRegistryImplementor registry;
	private JtaPlatform jtaPlatform;
	private JndiService jndiService;

	private Connection connection;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return OrientDBDialect.class;
	}

	@Override
	public void start() {
		LOG.info( "start" );
		try {
			PropertyReaderContext<String> jdbcUrlPropery = propertyReader.property( "javax.persistence.jdbc.url", String.class );
			if ( jdbcUrlPropery != null ) {
				String jdbcUrl = jdbcUrlPropery.getValue();
				LOG.info( "jdbcUrl:" + jdbcUrl );
				Class.forName( propertyReader.property( "javax.persistence.jdbc.driver", String.class ).getValue() ).newInstance();
				Properties info = new Properties();
				info.put( "user", propertyReader.property( "javax.persistence.jdbc.user", String.class ).getValue() );
				info.put( "password", propertyReader.property( "javax.persistence.jdbc.password", String.class ).getValue() );

				connection = DriverManager.getConnection( jdbcUrl, info );

			}

		}
		catch (Exception e) {
			throw LOG.unableToStartDatastoreProvider( e );
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void stop() {
		LOG.info( "stop" );
	}

	@Override
	public void configure(Map cfg) {
		LOG.info( "config map:" + cfg.toString() );
		propertyReader = new ConfigurationPropertyReader( cfg );

	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
		jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		LOG.info( "getSchemaDefinerType" );
		return OrientDBSchemaDefiner.class;
		// return super.getSchemaDefinerType();
	}

}
