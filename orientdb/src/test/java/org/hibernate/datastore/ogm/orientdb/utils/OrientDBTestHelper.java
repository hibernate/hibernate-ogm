/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.utils;

import com.orientechnologies.orient.core.id.ORecordId;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.datastore.ogm.orientdb.OrientDB;
import org.hibernate.datastore.ogm.orientdb.OrientDBDialect;
import org.hibernate.datastore.ogm.orientdb.OrientDBSimpleTest;
import org.hibernate.datastore.ogm.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.TestableGridDialect;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBTestHelper implements TestableGridDialect {

    private static final Logger LOG = Logger.getLogger(OrientDBTestHelper.class.getName());
        
        private static final String JDBC_URL = "jdbc:orient:".concat(OrientDBSimpleTest.MEMORY_TEST);
	private static OrientGraphNoTx graphNoTx;

	public OrientDBTestHelper() {
            LOG.info("call me");
		//BasicConfigurator.configure();
		// create OrientDB in memory
		graphNoTx = MemoryDBUtil.createDbFactory(OrientDBSimpleTest.MEMORY_TEST);

	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {            
		return TestHelper.getNumberOfEntities(sessionFactory);
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return TestHelper.getNumberOfAssociations(sessionFactory);
																			
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		return TestHelper.getNumberOfAssociations(sessionFactory,type);
																			
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tuple = new HashMap<String, Object>();
		GridDialect dialect = getDialect( sessionFactory );
		TupleSnapshot snapshot = dialect.getTuple( key, GridDialectOperationContexts.emptyTupleContext() ).getSnapshot();
		for ( String column : snapshot.getColumnNames() ) {
			tuple.put( column, snapshot.get( column ) );
		}
                return tuple;
	}
        

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		LOG.info("call dropSchemaAndDatabase! vertices: "+graphNoTx.countVertices());
                if (graphNoTx.countVertices()>0) {
                    MemoryDBUtil.recrateInMemoryDn(OrientDBSimpleTest.MEMORY_TEST);
                }
                
	}
    

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return readProperties();
	}
        
        private static Map<String, String> readProperties() {
		try {
			Properties hibProperties = new Properties();
			hibProperties.load( Thread.currentThread().getContextClassLoader().getResourceAsStream( "hibernate.properties" ) );
			Map<String, String> props = new HashMap<>();
			for ( Map.Entry<Object, Object> entry : hibProperties.entrySet() ) {
				props.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
                                LOG.info(entry.toString());
			}
			return Collections.unmodifiableMap( props );
		}
		catch (IOException e) {
			throw new RuntimeException( "Missing properties file: hibernate.properties" );
		}
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return OrientDB.class;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new OrientDBDialect( (OrientDBDatastoreProvider) datastoreProvider );
	}
        
        private static OrientDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( OrientDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with OrientDB, cannot extract underlying provider" );
		}
		return OrientDBDatastoreProvider.class.cast( provider );
	}
private static GridDialect getDialect(SessionFactory sessionFactory) {
		GridDialect dialect = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( GridDialect.class );
		return dialect;
	}


}
