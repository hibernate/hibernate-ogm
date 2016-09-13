/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public class NullableProtofieldEncoder {

	/**
	 * Wrap a ProtofieldEncoder function into another ProtofieldEncoder which adds it capabilities
	 * to deal with encoding of optional (nullable) values.
	 * @param baseEncoder the ProtofieldEncoder to use normally
	 * @param isNullable if true we'll wrap it, otherwise the baseEncoder is returned untouched.
	 * @return
	 */
	static <T> ProtofieldEncoder<T> makeNullableFieldEncoder(final ProtofieldEncoder<T> baseEncoder, final boolean isNullable) {
		if ( isNullable ) {
			return (ProtoStreamWriter outProtobuf, T value) -> {
				if ( value != null ) {
					baseEncoder.encode( outProtobuf, value );
				}
			};
		}
		else {
			return baseEncoder;
		}
	}

}
