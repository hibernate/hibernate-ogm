/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.sql.Time;

import org.infinispan.protostream.MessageMarshaller;

/**
 * @author Fabio Massimo Ercoli
 */
public class TimeProtofieldAccessor extends BaseProtofieldAccessor<Time> implements ProtofieldAccessor<Time> {

	public TimeProtofieldAccessor(int tag, String name, boolean nullable, String columnName) {
		super( tag, name, nullable, columnName,
				(MessageMarshaller.ProtoStreamWriter outProtobuf, Time value) -> outProtobuf.writeLong( name, value.getTime() ),
				(MessageMarshaller.ProtoStreamReader reader) -> {
					Long utcTimestamp = reader.readLong( name );
					if ( utcTimestamp != null ) {
						Time d = new Time( utcTimestamp );
						return d;
					}
					else {
						return null;
					}
				} );
	}

	@Override
	protected String getProtobufTypeName() {
		return "int64";
	}
}
