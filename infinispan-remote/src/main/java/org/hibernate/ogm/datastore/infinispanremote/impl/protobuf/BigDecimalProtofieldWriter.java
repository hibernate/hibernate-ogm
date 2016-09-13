/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * A BigDecimal is encoded as a byte array after converting to a BigInteger.
 * The byte array conversion of BigInteger is well defined.
 *
 * @see java.math.BigInteger#toByteArray
 */
public class BigDecimalProtofieldWriter extends BaseProtofieldWriter<BigDecimal> implements ProtofieldWriter<BigDecimal> {

	public BigDecimalProtofieldWriter(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, BigDecimal value) -> outProtobuf.writeBytes( name, value.toBigInteger().toByteArray() ),
				(ProtoStreamReader reader) -> {
					byte[] readBytes = reader.readBytes( name );
					if ( readBytes != null ) {
						BigInteger bi = new BigInteger( readBytes );
						BigDecimal bd = new BigDecimal( bi );
						return bd;
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
