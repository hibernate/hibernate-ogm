/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.map.MapDatastore;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapDialect;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class HashMapTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getEntityMap( sessionFactory ).size();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return getAssociationCache( sessionFactory ).size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return getEntityMap( sessionFactory ).get( key );
	}

	private static Map<EntityKey, Map<String, Object>> getEntityMap(SessionFactory sessionFactory) {
		MapDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getEntityMap();
	}

	private static MapDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( MapDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with MapDatastoreProvider, cannot extract underlying map" );
		}
		return MapDatastoreProvider.class.cast( provider );
	}

	private static Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache( SessionFactory sessionFactory) {
		MapDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getAssociationsMap();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// Nothing to do
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
		return configuration.configureOptionsFor( MapDatastore.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new MapDialect( (MapDatastoreProvider) datastoreProvider );
	}
}
