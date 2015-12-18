/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.math.BigInteger;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * A BigInteger is encoded as a byte array.
 * The byte array conversion of BigInteger is well defined.
 *
 * @see java.math.BigInteger#toByteArray
 */
public class BigIntegerProtofieldAccessor extends BaseProtofieldAccessor<BigInteger> implements ProtofieldAccessor<BigInteger> {

	public BigIntegerProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, BigInteger value) -> outProtobuf.writeBytes( name, value.toByteArray() ),
				(ProtoStreamReader reader) -> {
					byte[] readBytes = reader.readBytes( name );
					if ( readBytes != null ) {
						return new BigInteger( readBytes );
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
