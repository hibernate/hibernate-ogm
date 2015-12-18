/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.sequences;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.NextValueRequest;

/**
 * Both IdSourceType.SEQUENCE and IdSourceType.TABLE are treated the same, essentially as a TABLE.
 * With Hot Rod mapping, a TABLE maps to a Cache, so we can store multiple "sequences" in the same
 * Cache by identifying each one by name. Each name is a key, so depending on perspective you
 * might consider these as a named SEQUENCE rather than a TABLE strategy.
 *
 * We don't write primitives to the Cache but wrap them in proper Protobuf mapped messages to
 * ensure to avoid conflicts with other mapped elements and to apply the column names of user's choice.
 *
 * The org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect#supportsSequences
 * method returns 'false' so that we don't need a new Cache for each single sequence.
 *
 * See https://github.com/infinispan/infinispan/blob/master/client/hotrod-client/src/test/java/org/infinispan/client/hotrod/ReplaceWithVersionConcurrencyTest.java
 *
 * @see org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType
 * @author Sanne Grinovero
 */
public class HotRodSequenceHandler {

	private static final Log log = LoggerFactory.getLogger();

	private final InfinispanRemoteDatastoreProvider provider;
	private final ConcurrentMap<String,SequencesPerCache> sequencesPerCache = new ConcurrentHashMap<>();
	private final Map<String, SequenceTableDefinition> idSchemaPerName;

	public HotRodSequenceHandler(
			InfinispanRemoteDatastoreProvider infinispanRemoteDatastoreProvider,
			Map<String, SequenceTableDefinition> idSchemaPerName) {
		this.provider = infinispanRemoteDatastoreProvider;
		this.idSchemaPerName = idSchemaPerName;
	}

	public Number getSequenceValue(NextValueRequest request) {
		final String cacheName = request.getKey().getMetadata().getName();
		final SequencesPerCache sequencesSet = sequencesPerCache.computeIfAbsent( cacheName, ( k ) -> {
			SequenceTableDefinition sequenceTableDefinition = idSchemaPerName.get( cacheName );
			if ( sequenceTableDefinition == null ) {
				throw log.valueRequestedForUnknownSequence( request.getKey().getTable(), request.getKey().getColumnValue() );
			}
			return new SequencesPerCache(
					provider,
					sequenceTableDefinition,
					provider.getCache( cacheName )
			);
		}
		);
		return sequencesSet.getSequenceValue( request );
	}

}
