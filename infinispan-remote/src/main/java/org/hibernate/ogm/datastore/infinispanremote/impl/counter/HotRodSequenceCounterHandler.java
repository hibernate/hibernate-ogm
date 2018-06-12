/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.counter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.HotRodSequenceHandler;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * Handle generation value for id sources.
 *
 * Using {@link CounterHandler}
 * for the id source defined with {@link javax.persistence.SequenceGenerator}.
 * Delegating to base class {@link HotRodSequenceHandler}
 * for the id source defined with {@link javax.persistence.TableGenerator}.
 *
 * @author Fabio Massimo Ercoli
 */
public class HotRodSequenceCounterHandler extends HotRodSequenceHandler {

	private Map<String, CounterHandler> sequenceHandlers = new HashMap<>();

	public HotRodSequenceCounterHandler(InfinispanRemoteDatastoreProvider owner,
			OgmProtoStreamMarshaller marshaller,
			Map<String, SequenceTableDefinition> idSchemaPerName,
			Set<Sequence> sequences) {
		super( owner, marshaller, idSchemaPerName );
		for ( Sequence sequence : sequences ) {
			CounterHandler handler = new CounterHandler( owner.getManager(), sequence );
			sequenceHandlers.put( handler.getCounterName(), handler );
		}
	}

	@Override
	public Number getSequenceValue(NextValueRequest request) {
		IdSourceKey idSourceKey = request.getKey();
		if ( isSequenceGeneratorId( idSourceKey ) ) {
			return sequenceHandlers.get( idSourceKey.getTable() ).nextValue( request );
		}

		// else it is TableGenerator:
		return super.getSequenceValue( request );
	}

	private boolean isSequenceGeneratorId(IdSourceKey idSourceKey) {
		return isSequenceGeneratorId( idSourceKey.getMetadata() );
	}

	public static boolean isSequenceGeneratorId(IdSourceKeyMetadata idSourceKeyMetadata) {
		return IdSourceKeyMetadata.IdSourceType.SEQUENCE.equals( idSourceKeyMetadata.getType() );
	}
}
