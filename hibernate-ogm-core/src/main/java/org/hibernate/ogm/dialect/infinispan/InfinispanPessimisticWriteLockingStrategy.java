/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
		advCache.lock( new Key( lockable.getRootTableName(), id ) );
		//FIXME check the version number as well and raise an optimistic lock exception if there is an issue JPA 2 spec: 3.4.4.2
	}
}
