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

import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.client.hotrod.RemoteCache;

public class SequencesPerCache {

	private final RemoteCache<SequenceId, Long> remoteCache;
	private final ConcurrentMap<IdSourceKey,HotRodSequencer> sequencers = new ConcurrentHashMap<>();

	SequencesPerCache(RemoteCache<SequenceId, Long> remoteCache) {
		this.remoteCache = Objects.requireNonNull( remoteCache );
	}

	public Number getSequenceValue(NextValueRequest request) {
		IdSourceKey key = request.getKey();
		HotRodSequencer sequencer = sequencers.computeIfAbsent( key, v ->
			new HotRodSequencer( remoteCache, request )
		);
		return sequencer.getSequenceValue( request );
	}
}
