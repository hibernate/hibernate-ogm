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
 * Decided to encode it as a byte array type; on reading a longer sequence it will be truncated as we only read the first byte.
 */
public class ByteProtofieldWriter extends BaseProtofieldWriter<Byte> implements ProtofieldWriter<Byte> {

	public ByteProtofieldWriter(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Byte value) -> {
					byte[] array = { value.byteValue() };
					outProtobuf.writeBytes( name, array );
				},
				(ProtoStreamReader reader) -> {
					byte[] array = reader.readBytes( name );
					if ( array != null && array.length > 0 ) {
						return Byte.valueOf( array[0] );
					}
					return null;
				}
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "bytes";
	}

}
