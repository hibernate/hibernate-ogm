/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.util;

import com.orientechnologies.orient.core.id.ORecordId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.BasicConfigurator;
import org.hibernate.SessionFactory;
import org.hibernate.datastore.ogm.orientdb.OrientDB;
import org.hibernate.datastore.ogm.orientdb.OrientDBDialect;
import org.hibernate.datastore.ogm.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.utils.TestableGridDialect;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBTestHelper implements TestableGridDialect {

	private static final String JDBC_URL = "jdbc:orient:memory:test";
	private static Map<String, List<ORecordId>> classIdMap;

	public OrientDBTestHelper() {
		BasicConfigurator.configure();
		// create OrientDB in memory
		MemoryDBUtil.createDbFactory( "memory:test" );

	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getNumberOfEntities()" );
		long summ = 0;
		for ( Map.Entry<String, List<ORecordId>> entry : classIdMap.entrySet() ) {
			String key = entry.getKey();
			List<ORecordId> value = entry.getValue();
			for ( int i = 0; i < value.size(); i++ ) {
				ORecordId get = value.get( i );
				summ++;
			}

		}
		return classIdMap.size();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getNumberOfAssociations()" );
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getNumberOfAssociations()" );
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.extractEntityTuple()" );
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

	@Override
	public boolean backendSupportsTransactions() {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.backendSupportsTransactions()" );
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.dropSchemaAndDatabase()" );

		Map<String, ClassMetadata> metadata = sessionFactory.getAllClassMetadata();
		for ( Map.Entry<String, ClassMetadata> entry : metadata.entrySet() ) {
			String key = entry.getKey();
			ClassMetadata value = entry.getValue();
			System.err.println( "key: " + key );
			System.err.println( "value: " + value );
		}

	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getEnvironmentProperties()" );
		Map<String, String> properties = new HashMap<String, String>();
		properties.put( "javax.persistence.jdbc.url", JDBC_URL );
		properties.put( "javax.persistence.jdbc.driver", "com.orientechnologies.orient.jdbc.OrientJdbcDriver" );
		properties.put( "javax.persistence.jdbc.user", "admin" );
		properties.put( "javax.persistence.jdbc.password", "admin" );
		return properties;
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getDatastoreConfigurationType()" );
		return OrientDB.class;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		System.out.println( "org.hibernate.datastore.ogm.orientdb.util.OrientDBTestHelper.getGridDialect()" );
		return new OrientDBDialect( (OrientDBDatastoreProvider) datastoreProvider );
	}

}
