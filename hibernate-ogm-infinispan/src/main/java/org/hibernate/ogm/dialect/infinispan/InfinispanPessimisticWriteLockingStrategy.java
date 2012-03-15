/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.infinispan;

import java.io.Serializable;

import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.infinispan.AdvancedCache;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.persister.EntityKeyBuilder;
import org.hibernate.persister.entity.Lockable;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.*;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class InfinispanPessimisticWriteLockingStrategy implements LockingStrategy {
	private final LockMode lockMode;
	private final Lockable lockable;
	private volatile InfinispanDatastoreProvider provider;
	private static final Log log = LoggerFactory.make();
	protected final GridType identifierGridType;

	public InfinispanPessimisticWriteLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockMode = lockMode;
		this.lockable = lockable;
		TypeTranslator typeTranslator = lockable.getFactory().getServiceRegistry().getService( TypeTranslator.class );
		this.identifierGridType = typeTranslator.getType(lockable.getIdentifierType());
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
			throws StaleObjectStateException, JDBCException {
		AdvancedCache advCache = getProvider(session).getCache(ENTITY_STORE).getAdvancedCache();
		EntityKey key = EntityKeyBuilder.fromData(
				lockable.getRootTableName(),
				lockable.getRootTableIdentifierColumnNames(),
				identifierGridType,
				id,
				session );
		advCache.lock( key );
		//FIXME check the version number as well and raise an optimistic lock exception if there is an issue JPA 2 spec: 3.4.4.2
	}

	private InfinispanDatastoreProvider getProvider(SessionImplementor session) {
		if ( provider == null ) {
			DatastoreProvider service = session.getFactory().getServiceRegistry().getService(DatastoreProvider.class);
			if ( service instanceof InfinispanDatastoreProvider ) {
				provider = InfinispanDatastoreProvider.class.cast(service);
			}
			else {
				log.unexpectedDatastoreProvider(service.getClass(), InfinispanDatastoreProvider.class);
			}
		}
		return provider;
	}
}
