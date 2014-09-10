/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

import java.io.Serializable;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.Lockable;
import org.infinispan.AdvancedCache;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class InfinispanPessimisticWriteLockingStrategy implements LockingStrategy {
	private static final Log log = LoggerFactory.make();

	protected final GridType identifierGridType;

	private volatile InfinispanDatastoreProvider provider;
	private final LockMode lockMode;
	private final Lockable lockable;

	public InfinispanPessimisticWriteLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockMode = lockMode;
		this.lockable = lockable;
		TypeTranslator typeTranslator = lockable.getFactory().getServiceRegistry().getService( TypeTranslator.class );
		this.identifierGridType = typeTranslator.getType( lockable.getIdentifierType() );
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session)
			throws StaleObjectStateException, JDBCException {
		AdvancedCache advCache = getProvider( session ).getCache( ENTITY_STORE ).getAdvancedCache();
		EntityKey key = EntityKeyBuilder.fromData(
				( (OgmEntityPersister) lockable).getRootEntityKeyMetadata(),
				identifierGridType,
				id,
				session );
		advCache.lock( key );
		//FIXME check the version number as well and raise an optimistic lock exception if there is an issue JPA 2 spec: 3.4.4.2
	}

	private InfinispanDatastoreProvider getProvider(SessionImplementor session) {
		if ( provider == null ) {
			DatastoreProvider service = session.getFactory().getServiceRegistry().getService( DatastoreProvider.class );
			if ( service instanceof InfinispanDatastoreProvider ) {
				provider = InfinispanDatastoreProvider.class.cast( service );
			}
			else {
				log.unexpectedDatastoreProvider( service.getClass(), InfinispanDatastoreProvider.class );
			}
		}
		return provider;
	}
}
