/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.sequences;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.protostream.SerializationContext;

/**
 * This implements the atomic operations to model a source for named sequences.
 * The SequenceId represents the name of this sequence and is specific to a Remote Cache.
 *
 * The implementation uses optimistic locking and currently has no fallback strategy:
 * this is not suited for high contention scenarios. Not least, the Hot Rod client
 * API doesn't allow to issue both the CAS operation and return the new version
 * in case of failure, so the failure of an optimistic replace operation needs
 * to re-read the version, introducing additional delays and making further
 * failures more likely.
 *
 * TODO: see OGM-1161 to discuss options.
 *
 * @author Sanne Grinovero
 */
public final class HotRodSequencer {

	private static final Log log = LoggerFactory.getLogger();

	private final RemoteCache<SequenceId, Long> remoteCache;
	private final SerializationContext serContext;
	private final int increment;
	private final SequenceId id;

	private long lastKnownVersion = -1;
	private Long lastKnownRemoteValue = null;

	HotRodSequencer(
			RemoteCache<SequenceId, Long> remoteCache,
			SequenceTableDefinition sequenceTableDefinition,
			NextValueRequest initialRequest,
			SerializationContext serContext) {
				this.remoteCache = remoteCache;
				this.increment = initialRequest.getIncrement();
				this.serContext = serContext;
				this.id = new SequenceId( initialRequest.getKey().getColumnValue() );
	}

	Number getSequenceValue(NextValueRequest request) {
		try {
			OgmProtoStreamMarshaller.setCurrentSerializationContext( serContext );
			return getSequenceValueInternal( request );
		}
		finally {
			OgmProtoStreamMarshaller.setCurrentSerializationContext( null );
		}
	}

	private synchronized Number getSequenceValueInternal(NextValueRequest request) {
		if ( lastKnownRemoteValue == null ) {
			Long initialValue = (long) request.getInitialValue();
			Long previous = remoteCache.putIfAbsent( id, initialValue );
			//Side effects: initialize fields with first known values from remote
			getRemoteVersion();
			if ( previous == null ) {
				//if the putIfAbsent CAS was successfull, we can return already
				return lastKnownRemoteValue;
			}
		}
		//now to CAS:
		int casCycle = 0;
		while ( true ) {
			//TODO introduce a safety net against excessive spinning?
			//See https://hibernate.atlassian.net/browse/OGM-1161
			Long targetValue = Long.valueOf( lastKnownRemoteValue.longValue() + increment );
			boolean done = attemptCASWriteValue( targetValue );
			if ( done ) {
				return targetValue;
			}
			else {
				//On failure of CAS, refresh what we know about the remote version and value:
				getRemoteVersion();
				casCycle++;
				if ( casCycle % 10 == 0 ) {
					log.excessiveCasForSequencer( id.getSegmentName() );
				}
			}
		}
	}

	private boolean attemptCASWriteValue(final Long targetValue) {
		boolean success = remoteCache.replaceWithVersion( id, targetValue, lastKnownVersion );
		if ( success ) {
			lastKnownVersion++;
			lastKnownRemoteValue = targetValue;
		}
		return success;
	}

	private void getRemoteVersion() {
		VersionedValue<Long> versioned = remoteCache.getVersioned( id );
		if ( versioned == null ) {
			//Critical failure (only happens if the Infinispan servers lost data)
			//TODO
		}
		lastKnownVersion = versioned.getVersion();
		lastKnownRemoteValue = (Long) versioned.getValue();
	}

}
