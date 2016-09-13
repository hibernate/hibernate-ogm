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
 * There is no explicit "char" support in Protostream.
 * Decided to encode it as a 'string' type; on reading a longer string will be truncated as we only read the first character.
 */
public class CharacterProtofieldWriter extends BaseProtofieldWriter<Character> implements ProtofieldWriter<Character> {

	public CharacterProtofieldWriter(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Character value) -> outProtobuf.writeString( name, "" + value ),
				(ProtoStreamReader reader) -> {
					String valueAsString = reader.readString( name );
					if ( valueAsString != null && valueAsString.length() > 0 ) {
						return valueAsString.charAt( 0 );
					}
					return null;
				}
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "string";
	}

}
