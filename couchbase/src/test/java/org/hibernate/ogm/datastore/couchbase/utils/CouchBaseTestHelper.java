/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.TestableGridDialect;

public class CouchBaseTestHelper implements TestableGridDialect {

	static {
		initEnvironmentProperties();
	}

	public static void initEnvironmentProperties() {
		// Read host and port from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String couchbaseHostName = System.getenv( "COUCHBASE_HOSTNAME" );
		if ( isNotNull( couchbaseHostName ) ) {
			System.getProperties().setProperty( OgmProperties.HOST, couchbaseHostName );
		}
		String couchbasePort = System.getenv( "COUCHBASE_PORT" );
		if ( isNotNull( couchbasePort ) ) {
			System.getProperties().setProperty( OgmProperties.PORT, couchbasePort );
		}
	}

	private static boolean isNotNull(String property) {
		return property != null && property.length() > 0 && ! "null".equals( property );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean backendSupportsTransactions() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		// TODO Auto-generated method stub
		return null;
	}

}
