/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.map.MapDatastore;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapDialect;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class HashMapTestHelper extends BaseGridDialectTestHelper implements GridDialectTestHelper {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getEntityMap( sessionFactory ).size();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		return getAssociationCache( sessionFactory ).size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		return getEntityMap( session.getSessionFactory() ).get( key );
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
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// Nothing to do
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new MapDialect( (MapDatastoreProvider) datastoreProvider );
	}

	@Override
	public Class<MapDatastore> getDatastoreConfigurationType() {
		return MapDatastore.class;
	}
}
