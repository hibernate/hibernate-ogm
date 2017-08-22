/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.dialect.impl;

import java.io.Serializable;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanEmbeddedDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.persister.entity.Lockable;
import org.infinispan.AdvancedCache;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class InfinispanPessimisticWriteLockingStrategy<EK> implements LockingStrategy {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	protected final GridType identifierGridType;

	private final LockMode lockMode;
	private final Lockable lockable;

	private final InfinispanEmbeddedDatastoreProvider provider;

	public InfinispanPessimisticWriteLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockMode = lockMode;
		this.lockable = lockable;
		TypeTranslator typeTranslator = lockable.getFactory().getServiceRegistry().getService( TypeTranslator.class );
		this.identifierGridType = typeTranslator.getType( lockable.getIdentifierType() );

		provider = getProvider( lockable.getFactory() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void lock(Serializable id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, JDBCException {
		LocalCacheManager<EK, ?, ?> cacheManager = getCacheManager();
		KeyProvider<EK, ?, ?> keyProvider = getKeyProvider();

		AdvancedCache<EK, ?> advCache = cacheManager.getEntityCache( ( (OgmEntityPersister) lockable ).getRootEntityKeyMetadata() ).getAdvancedCache();
		EntityKey key = EntityKeyBuilder.fromData(
				( (OgmEntityPersister) lockable ).getRootEntityKeyMetadata(),
				identifierGridType,
				id,
				session );
		advCache.lock( keyProvider.getEntityCacheKey( key ) );
		//FIXME check the version number as well and raise an optimistic lock exception if there is an issue JPA 2 spec: 3.4.4.2
	}

	private static InfinispanEmbeddedDatastoreProvider getProvider(SessionFactoryImplementor factory) {
		DatastoreProvider service = factory.getServiceRegistry().getService( DatastoreProvider.class );

		if ( service instanceof InfinispanEmbeddedDatastoreProvider ) {
			return InfinispanEmbeddedDatastoreProvider.class.cast( service );
		}
		else {
			throw log.unexpectedDatastoreProvider( service.getClass(), InfinispanEmbeddedDatastoreProvider.class );
		}
	}

	@SuppressWarnings("unchecked")
	private LocalCacheManager<EK, ?, ?> getCacheManager() {
		return (LocalCacheManager<EK, ?, ?>) provider.getCacheManager();
	}

	@SuppressWarnings("unchecked")
	private KeyProvider<EK, ?, ?> getKeyProvider() {
		return (KeyProvider<EK, ?, ?>) provider.getKeyProvider();
	}
}
