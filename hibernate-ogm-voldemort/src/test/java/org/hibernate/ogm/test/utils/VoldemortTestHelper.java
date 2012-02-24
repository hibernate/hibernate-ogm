package org.hibernate.ogm.test.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.voldemort.impl.VoldemortDatastoreProvider;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

public class VoldemortTestHelper implements TestableGridDialect {

	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		return getEntityMap(sessionFactory).size();
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		return getAssociationCache(sessionFactory).size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(
			SessionFactory sessionFactory, EntityKey key) {
		return getEntityMap(sessionFactory).get(key);
	}

	private static Map<EntityKey, Map<String, Object>> getEntityMap(
			SessionFactory sessionFactory) {
		VoldemortDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getEntityMap();
	}

	private static VoldemortDatastoreProvider getProvider(
			SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory)
				.getServiceRegistry().getService(DatastoreProvider.class);
		if (!(DatastoreProvider.class.isInstance(provider))) {
			throw new RuntimeException(
					"Not testing with VoldemortDatastoreProvider, cannot extract underlying map");
		}
		return VoldemortDatastoreProvider.class.cast(provider);
	}

	private static Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache(
			SessionFactory sessionFactory) {
		VoldemortDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getAssociationsMap();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

}
