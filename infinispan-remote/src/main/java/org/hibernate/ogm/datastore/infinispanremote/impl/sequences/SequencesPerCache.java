/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.sequences;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.SerializationContext;

public class SequencesPerCache {

	private final RemoteCache<SequenceId, Long> remoteCache;
	private final SequenceTableDefinition sequenceTableDefinition;
	private final ConcurrentMap<IdSourceKey,HotRodSequencer> sequencers = new ConcurrentHashMap<>();
	private final SerializationContext serializationContext;
	private final OgmProtoStreamMarshaller marshaller;

	SequencesPerCache(
			InfinispanRemoteDatastoreProvider provider,
			SequenceTableDefinition sequenceTableDefinition,
			RemoteCache<SequenceId, Long> remoteCache,
			OgmProtoStreamMarshaller marshaller) {
		this.sequenceTableDefinition = Objects.requireNonNull( sequenceTableDefinition );
		this.remoteCache = Objects.requireNonNull( remoteCache );
		this.serializationContext = provider.getSerializationContextForSequences( sequenceTableDefinition );
		this.marshaller = marshaller;
	}

	public Number getSequenceValue(NextValueRequest request) {
		IdSourceKey key = request.getKey();
		HotRodSequencer sequencer = sequencers.computeIfAbsent( key,  v -> {
			return new HotRodSequencer( remoteCache,
					sequenceTableDefinition, request, serializationContext, marshaller );
		} );
		return sequencer.getSequenceValue( request );
	}

}
