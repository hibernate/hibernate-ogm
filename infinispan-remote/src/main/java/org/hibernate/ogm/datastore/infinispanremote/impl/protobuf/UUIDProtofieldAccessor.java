/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.util.UUID;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * UUID are commonly encoded as 'string' in Protobuf
 */
public final class UUIDProtofieldAccessor extends BaseProtofieldAccessor<UUID> implements ProtofieldAccessor<UUID> {

	public UUIDProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, UUID value) -> outProtobuf.writeString( name, value.toString() ),
				(ProtoStreamReader reader) -> UUID.fromString( reader.readString( name ) )
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "string";
	}

}
