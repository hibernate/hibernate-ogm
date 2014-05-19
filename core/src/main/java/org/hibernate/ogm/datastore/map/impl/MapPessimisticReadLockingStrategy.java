/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.map.impl;

import java.io.Serializable;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class MapPessimisticReadLockingStrategy extends MapPessimisticWriteLockingStrategy implements LockingStrategy {

	public MapPessimisticReadLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session) throws StaleObjectStateException, JDBCException {
		MapDatastoreProvider dataStore = getProvider( session );
		EntityKey key = EntityKeyBuilder.fromData(
				( (OgmEntityPersister) lockable ).getRootEntityKeyMetadata(),
				identifierGridType,
				id,
				session );
		dataStore.readLock( key, timeout );
	}
}
