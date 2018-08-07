/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.time.LocalDateTime;

import org.infinispan.protostream.MessageMarshaller;

/**
 * @author Fabio Massimo Ercoli
 */
public class LocalDateTimeProtofieldAccessor extends BaseProtofieldAccessor<LocalDateTime> implements ProtofieldAccessor<LocalDateTime> {

	public LocalDateTimeProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(MessageMarshaller.ProtoStreamWriter outProtobuf, LocalDateTime value) ->
					outProtobuf.writeString( name, value.toString()
				),
				(MessageMarshaller.ProtoStreamReader reader) -> {
					String stringValue = reader.readString( name );
					return ( stringValue == null ) ? null :
							LocalDateTime.parse( stringValue );
				}
		);
	}

	@Override
	protected String getProtobufTypeName() {
		return "string";
	}
}
