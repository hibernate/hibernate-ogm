/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * There is no explicit "byte" support in Protostream.
 * Decided to encode it as a int type; on reading an int will be truncated as a byte,
 * using the {@link Integer#byteValue()} method.
 */
public class ByteProtofieldAccessor extends BaseProtofieldAccessor<Byte> implements ProtofieldAccessor<Byte> {

	public ByteProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Byte value) -> outProtobuf.writeInt( name, value ),
				(ProtoStreamReader reader) -> {
					Integer storedValue = reader.readInt( name );
					return ( storedValue == null ) ? null : storedValue.byteValue();
				}
		);

	}

	@Override
	protected String getProtobufTypeName() {
		return "int32";
	}
}
