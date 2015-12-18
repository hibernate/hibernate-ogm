/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.util.Calendar;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public class CalendarDateProtofieldWriter extends BaseProtofieldWriter<Calendar> implements ProtofieldWriter<Calendar> {

	public CalendarDateProtofieldWriter(int tag, final String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(ProtoStreamWriter outProtobuf, Calendar value) -> outProtobuf.writeLong( name, value.getTimeInMillis() ),
				(ProtoStreamReader reader) -> {
					//TODO should we map this as a composite object, to encode both the utcTimestamp and the timezone?
					Long utcTimestamp = reader.readLong( name );
					if ( utcTimestamp != null ) {
						Calendar c = Calendar.getInstance();
						c.setTimeInMillis( utcTimestamp );
						return c;
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
