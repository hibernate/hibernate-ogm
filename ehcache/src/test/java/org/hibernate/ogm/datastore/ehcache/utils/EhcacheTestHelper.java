/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.ehcache.Ehcache;
import org.hibernate.ogm.datastore.ehcache.EhcacheDialect;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableEntityKey;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.TestableGridDialect;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getProvider( sessionFactory ).getEntityCache().getSize();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return getProvider( sessionFactory ).getAssociationCache().getSize();
	}

	@Override
	public Map<String,Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		@SuppressWarnings("unchecked")
		Map<String, Object> tuple = (Map<String, Object>) getProvider( sessionFactory )
				.getEntityCache()
				.get( new SerializableEntityKey( key ) )
				.getObjectValue();

		return tuple;
	}

	private static EhcacheDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry()
				.getService( DatastoreProvider.class );
		if ( !( EhcacheDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Ehcache, cannot extract underlying cache" );
		}
		return EhcacheDatastoreProvider.class.cast( provider );
	}

	/**
	 * TODO - EHCache _is_ transactional. Turn this on. We could turn on XA or Local.
	 * Local will be faster. We will pick this up from the cache config.
	 *
	 * @return
	 */
	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		//Nothing to do
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Ehcache.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new EhcacheDialect( (EhcacheDatastoreProvider) datastoreProvider );
	}
}
