/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

public abstract class BaseProtofieldWriter<T> implements ProtofieldWriter<T> {

	protected final int fieldNumber;
	protected final String name;
	protected final String columnName;
	protected final boolean nullable;
	protected final ProtofieldEncoder<T> writingFunction;
	protected final ProtofieldDecoder<T> readingFunction;

	public BaseProtofieldWriter(int fieldLabel, String fieldName, boolean nullable, String columnName,
			ProtofieldEncoder<T> writingFunction, ProtofieldDecoder<T> readingFunction) {
		this.fieldNumber = fieldLabel;
		this.name = fieldName;
		this.columnName = columnName;
		this.nullable = nullable;
		this.readingFunction = readingFunction;
		this.writingFunction = NullableProtofieldEncoder.makeNullableFieldEncoder( writingFunction, nullable );
	}

	@Override
	public void writeTo(ProtoStreamWriter outProtobuf, T value) throws IOException {
		writingFunction.encode( outProtobuf, value );
	}

	@Override
	public T read(ProtoStreamReader reader) throws IOException {
		return readingFunction.read( reader );
	}

	@Override
	public void exportProtobufFieldDefinition(StringBuilder sb) {
		if ( nullable ) {
			sb.append( "\n\toptional " );
		}
		else {
			sb.append( "\n\trequired " );
		}
		sb.append( getProtobufTypeName() )
			.append( " " )
			.append( name )
			.append( " = " )
			.append( fieldNumber )
			.append( ";" );
	}

	protected abstract String getProtobufTypeName();

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public String getProtobufName() {
		return name;
	}

}
