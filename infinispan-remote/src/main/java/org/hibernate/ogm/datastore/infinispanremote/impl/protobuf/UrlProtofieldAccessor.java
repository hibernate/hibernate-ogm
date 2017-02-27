/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.net.URL;

import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public final class UrlProtofieldAccessor extends BaseProtofieldAccessor<URL> implements ProtofieldAccessor<URL> {

	public UrlProtofieldAccessor(final int tag, String name, boolean nullable, final String columnName) {
		super( tag, name, nullable, columnName,
			(ProtoStreamWriter outProtobuf, URL value) -> outProtobuf.writeString( name, UrlTypeDescriptor.INSTANCE.toString( value ) ),
			(ProtoStreamReader reader) -> {
				String readString = reader.readString( name );
				if ( readString != null ) {
					return UrlTypeDescriptor.INSTANCE.fromString( readString );
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
