/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public final class LongProtofieldAccessor extends BaseProtofieldAccessor<Long> implements ProtofieldAccessor<Long> {

	public LongProtofieldAccessor(final int tag, final String name, final boolean nullable, final String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Long value) -> outProtobuf.writeLong( name, value ),
				(ProtoStreamReader reader) -> reader.readLong( name )
			);
	}

	@Override
	protected String getProtobufTypeName() {
		return "int64";
	}

}
