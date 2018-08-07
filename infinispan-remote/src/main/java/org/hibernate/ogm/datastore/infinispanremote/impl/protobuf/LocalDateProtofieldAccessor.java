/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.time.LocalDate;

import org.infinispan.protostream.MessageMarshaller;

/**
 * @author Fabio Massimo Ercoli
 */
public class LocalDateProtofieldAccessor extends BaseProtofieldAccessor<LocalDate> implements ProtofieldAccessor<LocalDate> {

	public LocalDateProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(MessageMarshaller.ProtoStreamWriter outProtobuf, LocalDate value) ->
					outProtobuf.writeString( name, value.toString()
				),
				(MessageMarshaller.ProtoStreamReader reader) -> {
					String stringValue = reader.readString( name );
					return ( stringValue == null ) ? null :
							LocalDate.parse( stringValue );
				}
		);
	}

	@Override
	protected String getProtobufTypeName() {
		return "string";
	}
}
