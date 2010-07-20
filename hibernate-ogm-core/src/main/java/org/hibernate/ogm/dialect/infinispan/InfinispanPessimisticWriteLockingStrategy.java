package org.hibernate.ogm.dialect.infinispan;

import java.io.Serializable;

import org.infinispan.AdvancedCache;

import org.hibernate.EntityMode;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Emmanuel Bernard
 */
public class InfinispanPessimisticWriteLockingStrategy implements LockingStrategy {
	private final LockMode lockMode;
	private final Lockable lockable;

	public InfinispanPessimisticWriteLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockMode = lockMode;
		this.lockable = lockable;
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
			throws StaleObjectStateException, JDBCException {
		AdvancedCache advCache = GridMetadataManagerHelper.getEntityCache( session.getFactory() ).getAdvancedCache();
		advCache.lock( new Key( lockable.getMappedClass( EntityMode.POJO ), id ) );
	}
}
