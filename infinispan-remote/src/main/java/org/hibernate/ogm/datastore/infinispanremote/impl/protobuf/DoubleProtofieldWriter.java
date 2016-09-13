/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public final class DoubleProtofieldWriter extends BaseProtofieldWriter<Double> implements ProtofieldWriter<Double> {

	public DoubleProtofieldWriter(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Double value) -> outProtobuf.writeDouble( name, value ),
				(ProtoStreamReader reader) -> reader.readDouble( name )
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "double";
	}

}
