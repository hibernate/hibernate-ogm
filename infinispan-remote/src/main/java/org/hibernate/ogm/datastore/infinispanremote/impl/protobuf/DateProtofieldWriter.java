/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.util.Date;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public class DateProtofieldWriter extends BaseProtofieldWriter<Date> implements ProtofieldWriter<Date> {

	public DateProtofieldWriter(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Date value) -> outProtobuf.writeLong( name, value.getTime() ),
				(ProtoStreamReader reader) -> {
					Long utcTimestamp = reader.readLong( name );
					if ( utcTimestamp != null ) {
						Date d = new Date();
						d.setTime( utcTimestamp );
						return d;
					}
					else {
						return null;
					}
				}
				);
	}

	@Override
	protected String getProtobufTypeName() {
		return "int64";
	}

}
