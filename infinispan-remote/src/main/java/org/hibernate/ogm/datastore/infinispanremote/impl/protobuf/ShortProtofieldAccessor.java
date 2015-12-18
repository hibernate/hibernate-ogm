/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * Short instances are best encoded as int32, but we throw an exception if the read value doesn't
 * actually fit in the acceptable ranges for a Short.
 */
public final class ShortProtofieldAccessor extends BaseProtofieldAccessor<Short> implements ProtofieldAccessor<Short> {

	private static final Log LOG = LoggerFactory.getLogger();

	public ShortProtofieldAccessor(int fieldNumber, String name, boolean nullable, String columnName) {
		super( fieldNumber, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Short value) -> outProtobuf.writeInt( name, value ),
				(ProtoStreamReader reader) -> {
					Integer readInt = reader.readInt( name );
					if ( readInt != null ) {
						int truncated = Math.min( Math.max( readInt, Short.MIN_VALUE ), Short.MAX_VALUE );
						if ( truncated != readInt ) {
							throw LOG.truncatingShortOnRead( readInt, name );
						}
						return (short) truncated;
					}
					return null;
				}
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "int32";
	}

}
