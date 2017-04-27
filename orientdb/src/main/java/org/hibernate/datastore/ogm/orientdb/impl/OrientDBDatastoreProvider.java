/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.impl;

import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import org.hibernate.datastore.ogm.orientdb.OrientDBDialect;
import org.hibernate.datastore.ogm.orientdb.logging.impl.Log;
import org.hibernate.datastore.ogm.orientdb.logging.impl.LoggerFactory;
import org.hibernate.datastore.ogm.orientdb.utils.MemoryDBUtil;
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
        private static OrientGraphFactory factory;
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
				LOG.warn("jdbcUrl:" + jdbcUrl );
				Class.forName( propertyReader.property( "javax.persistence.jdbc.driver", String.class ).getValue() ).newInstance();
				Properties info = new Properties();
				info.put( "user", propertyReader.property( "javax.persistence.jdbc.user", String.class ).getValue() );
				info.put( "password", propertyReader.property( "javax.persistence.jdbc.password", String.class ).getValue() );
                                createInMemoryDB(jdbcUrl);

				connection = DriverManager.getConnection( jdbcUrl, info );

			}

		}
		catch (Exception e) {
			throw LOG.unableToStartDatastoreProvider( e );
		}
	}
        
        
        private static void createInMemoryDB(String jdbcUrl) {
            String orientDbUrl = jdbcUrl.substring("jdbc:orient:".length());
            
            if (orientDbUrl.startsWith("memory") ) {
                MemoryDBUtil.createDbFactory(orientDbUrl);
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
                if (MemoryDBUtil.getOrientGraphFactory()!=null) {
                    if (MemoryDBUtil.getOrientGraphFactory().exists()) {
                        MemoryDBUtil.getOrientGraphFactory().close();
                        MemoryDBUtil.getOrientGraphFactory().drop();
                    }
                }
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
		return OrientDBSchemaDefiner.class;
		// return super.getSchemaDefinerType();
	}

}
