/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtoStreamMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.ogm.model.impl.EntityKeyBuilder;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.infinispan.client.hotrod.impl.transaction.TransactionalRemoteCacheImpl;

/**
 * Counts the entities where {@link org.infinispan.client.hotrod.RemoteCache} is Transactional.
 * <p>
 * We can't use {@link TransactionalRemoteCacheImpl#size()},
 * because the method isn't transactional in a way they don't interact with the transaction's data.
 * <p>
 * The algorithm is in not suitable for production code.
 * Used in {@link InfinispanRemoteTestHelper} with a test scope.
 *
 * @author Fabio Massimo Ercoli
 * @see TransactionalRemoteCacheImpl
 */
public class TransactionalRemoteCacheCounter {

	public static int count(OgmEntityPersister persister, TransactionalRemoteCacheImpl cache, ProtoStreamMappingAdapter mapper,
			Session currentSession) {
		if ( cache.getTransactionManager() == null ) {
			return cache.size();
		}

		final Set<Object> committedKeys = new HashSet<>();
		cache.keySet().forEach( committedKey -> {
			Boolean stillPresentInLocalTransaction = mapper.withinCacheEncodingContext( c -> c.containsKey( committedKey ) );
			if ( stillPresentInLocalTransaction ) {
				// skip count of removed entries
				committedKeys.add( committedKey );
			}
		} );

		// count all committed keys not removed by the local transaction
		Integer counter = new Integer( committedKeys.size() );

		OgmSessionImpl ogmSession = (OgmSessionImpl) currentSession;
		PersistenceContext persistenceContext = ogmSession.getPersistenceContext();

		// iterate persistence context entities
		for ( Object key : persistenceContext.getEntitiesByKey().keySet() ) {
			EntityKey entityKey = (EntityKey) key;
			if ( !(persister.getEntityName().equals( entityKey.getEntityName() ) ) ) {
				continue;
			}

			ProtostreamId entryKey = convertEntityKey( entityKey, ogmSession, mapper );

			// count all new entities
			if ( !committedKeys.contains( entryKey ) ) {
				counter++;
			}
		}
		return counter;
	}

	private static ProtostreamId convertEntityKey(EntityKey entityKey, OgmSessionImpl ogmSession, ProtoStreamMappingAdapter mapper) {
		MetamodelImplementor metamodel = ogmSession.getFactory().getMetamodel();
		Serializable identifier = entityKey.getIdentifier();
		EntityPersister persister = metamodel.locateEntityPersister( entityKey.getEntityName() );
		org.hibernate.ogm.model.key.spi.EntityKey ogmEntityKey = EntityKeyBuilder.fromPersister( (OgmEntityPersister) persister, identifier, ogmSession );
		return mapper.createIdPayload( ogmEntityKey.getColumnNames(), ogmEntityKey.getColumnValues() );
	}
}
