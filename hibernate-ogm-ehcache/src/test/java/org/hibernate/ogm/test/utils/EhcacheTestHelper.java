package org.hibernate.ogm.test.utils;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.grid.EntityKey;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {
	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
