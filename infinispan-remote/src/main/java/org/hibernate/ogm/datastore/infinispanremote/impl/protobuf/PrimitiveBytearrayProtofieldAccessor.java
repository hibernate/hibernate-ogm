/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public class PrimitiveBytearrayProtofieldAccessor extends BaseProtofieldAccessor<byte[]> implements ProtofieldAccessor<byte[]> {

	public PrimitiveBytearrayProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, byte[] value) -> outProtobuf.writeBytes( name, value ),
				(ProtoStreamReader reader) -> reader.readBytes( name )
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "bytes";
	}

}
