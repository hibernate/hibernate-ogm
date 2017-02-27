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

/**
 * Protostream requires us to pass the actual type from the mapped Enum,
 * but OGM pre-maps these to String so se have to re-hydrate it from
 * String to map it to native enums.
 * See also: https://developers.google.com/protocol-buffers/docs/proto#enum
 * @author Sanne Grinovero
 */
public class EnumProtofieldAccessor implements ProtofieldAccessor<Enum> {

	private final int tag;
	private final String name;
	private final Class<? extends Enum> type;
	private final String columnName;
	private final boolean nullable;

	public EnumProtofieldAccessor(int tag, String name, boolean nullable, Class<? extends Enum> type, String columnName) {
		this.tag = tag;
		this.name = name;
		this.nullable = nullable;
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public void writeTo(ProtoStreamWriter outProtobuf, Enum value) throws IOException {
		outProtobuf.writeObject( name, value, type );
	}

	@Override
	public Enum read(ProtoStreamReader reader) throws IOException {
		return reader.readObject( name, type );
	}

	@Override
	public void collectTypeDefinitions(TypeDeclarationsCollector typesDefCollector) {
		typesDefCollector.createTypeDefinition( new EnumTypeDefinition( type ) );
	};

	@Override
	public void exportProtobufFieldDefinition(StringBuilder sb) {
		if ( nullable ) {
			sb.append( "\n\toptional " );
		}
		else {
			sb.append( "\n\trequired " );
		}
		sb.append( type.getSimpleName() );
		sb.append( " " );
		sb.append( name );
		sb.append( " = " );
		sb.append( tag );
		sb.append( ";" );
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public String getProtobufName() {
		return name;
	}

	private static final class EnumTypeDefinition implements TypeDefinition {

		private final Class<? extends Enum> type;

		public EnumTypeDefinition(Class<? extends Enum> type) {
			if ( type == null ) {
				throw new NullPointerException( "The 'type' parameter shall not be null" );
			}
			this.type = type;
		}

		@Override
		public void exportProtobufTypeDefinition(StringBuilder sb) {
			Enum[] enumConstants = type.getEnumConstants();
			sb.append( "\nenum " );
			sb.append( type.getSimpleName() );
			sb.append( " {" );
			for ( int i = 0; i < enumConstants.length; i++ ) {
				sb.append( "\n\t" );
				sb.append( enumConstants[i].name() );
				sb.append( " = " );
				sb.append( i );
				sb.append( ";" );
			}
			sb.append( "\n}\n" );
		}

		@Override
		public String getTypeName() {
			return type.getSimpleName();
		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			else if ( obj == null ) {
				return false;
			}
			else if ( EnumTypeDefinition.class != obj.getClass() ) {
				return false;
			}
			else {
				EnumTypeDefinition other = (EnumTypeDefinition) obj;
				return type.equals( other.type );
			}
		}

	}

}
