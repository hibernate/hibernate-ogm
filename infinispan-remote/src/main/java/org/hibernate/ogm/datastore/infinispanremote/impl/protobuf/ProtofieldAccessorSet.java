/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamMappedField;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.ProtobufFieldConsumer;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.ProtobufTypeConsumer;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SanitationUtils;
import org.hibernate.ogm.type.descriptor.impl.AttributeConverterGridTypeDescriptorAdaptor;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.impl.AttributeConverterGridTypeAdaptor;
import org.hibernate.ogm.type.impl.BigDecimalType;
import org.hibernate.ogm.type.impl.BigIntegerType;
import org.hibernate.ogm.type.impl.BooleanType;
import org.hibernate.ogm.type.impl.ByteType;
import org.hibernate.ogm.type.impl.CalendarDateType;
import org.hibernate.ogm.type.impl.CalendarType;
import org.hibernate.ogm.type.impl.CharacterType;
import org.hibernate.ogm.type.impl.DateType;
import org.hibernate.ogm.type.impl.DoubleType;
import org.hibernate.ogm.type.impl.EntityType;
import org.hibernate.ogm.type.impl.EnumType;
import org.hibernate.ogm.type.impl.FloatType;
import org.hibernate.ogm.type.impl.IntegerType;
import org.hibernate.ogm.type.impl.LongType;
import org.hibernate.ogm.type.impl.NumericBooleanType;
import org.hibernate.ogm.type.impl.PrimitiveByteArrayType;
import org.hibernate.ogm.type.impl.SerializableAsByteArrayType;
import org.hibernate.ogm.type.impl.ShortType;
import org.hibernate.ogm.type.impl.StringType;
import org.hibernate.ogm.type.impl.TimeType;
import org.hibernate.ogm.type.impl.TimestampType;
import org.hibernate.ogm.type.impl.TrueFalseType;
import org.hibernate.ogm.type.impl.UUIDType;
import org.hibernate.ogm.type.impl.UrlType;
import org.hibernate.ogm.type.impl.YesNoType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

public class ProtofieldAccessorSet {

	//Counter to assign unique Tag ids in protobuf. First to be assigned is '1'.
	private int uniqueTagAssigningCounter = 0;
	private List<UnsafeProtofield> orderedFields = new ArrayList<>();
	private Map<String,UnsafeProtofield> fieldsPerORMName = new HashMap<>();
	private Map<String,UnsafeProtofield> fieldsPerProtobufName = new HashMap<>();

	public void addMapping(String ormMappedName, GridType gridType, Type ormType, boolean nullable) {
		final String name = SanitationUtils.convertNameSafely( ormMappedName );
		uniqueTagAssigningCounter++;
		gridType = extractGridTypeOnRecursiveTypes( gridType, ormType );
		if ( gridType instanceof StringType ) {
			add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof IntegerType ) {
			add( new IntegerProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof LongType ) {
			add( new LongProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof DoubleType ) {
			add( new DoubleProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof UUIDType ) {
			add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof CalendarDateType ) {
			add( new CalendarDateProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof CalendarType ) {
			//TODO same as CalendarDateType ?
			add( new CalendarDateProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof DateType  ) {
			add( new DateProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof TimestampType ) {
			//TODO same as DateType?
			add( new DateProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof TimeType ) {
			//TODO same as DateType?
			add( new DateProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof PrimitiveByteArrayType  ) {
			add( new PrimitiveBytearrayProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof SerializableAsByteArrayType ) {
			//TODO same as PrimitiveByteArrayType?
			add( new PrimitiveBytearrayProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof CharacterType ) {
			add( new CharacterProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof ByteType ) {
			add( new ByteProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof BooleanType ) {
			add( new BooleanProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof TrueFalseType ) {
			add( new CharacterProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof YesNoType ) {
			add( new CharacterProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof NumericBooleanType ) {
			add( new IntegerProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof BigDecimalType ) {
			add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof UrlType ) {
			add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof ShortType ) {
			add( new ShortProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof BigIntegerType ) {
			add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof FloatType ) {
			add( new FloatProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
		}
		else if ( gridType instanceof EnumType ) {
			EnumType etype = (EnumType) gridType;
			if ( etype.isOrdinal() ) {
				add( new IntegerProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
			}
			else {
				add( new StringProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, ormMappedName ) );
				/* FIXME Alternative: Support of native Enum mapping in protobuf:
				if ( ormType instanceof CustomType ) {
					CustomType customOrmType = (CustomType) ormType;
					UserType userType = customOrmType.getUserType();
					org.hibernate.type.EnumType enumtype = (org.hibernate.type.EnumType) userType;
					Class returnedClass = enumtype.returnedClass();
					add( new EnumProtofieldAccessor( uniqueTagAssigningCounter, name, nullable, returnedClass, ormMappedName ) );
				}
				else {
					throw new AssertionFailure( "Type not implemented yet! " );
				} */
			}
		}
		else if ( gridType instanceof EntityType ) {
			throw new AssertionFailure( "EntityType not implemented yet! " + gridType.getName()  );
		}
		else {
			throw new AssertionFailure( "Type not implemented yet! " + gridType.getName()  );
		}
	}

	private GridType extractGridTypeOnRecursiveTypes(GridType gridType, Type ormType) {
		if ( gridType instanceof AttributeConverterGridTypeAdaptor ) {
			AttributeConverterGridTypeAdaptor acgta = (AttributeConverterGridTypeAdaptor) gridType;
			GridTypeDescriptor descriptor = acgta.getGridTypeDescriptor();
			if ( descriptor instanceof AttributeConverterGridTypeDescriptorAdaptor ) {
				AttributeConverterGridTypeDescriptorAdaptor internalType = (AttributeConverterGridTypeDescriptorAdaptor) descriptor;
				return internalType.unwrapTargetGridType();
			}
		}
		return gridType;
	}

	public void forEachProtobufFieldExporter(ProtobufFieldConsumer action) {
		orderedFields.forEach( action );
	}

	public void forEach(ProtobufTypeConsumer action) {
		orderedFields.forEach( action );
	}

	public void forEachProtostreamMappedField(Consumer<ProtostreamMappedField> action) {
		orderedFields.forEach( action );
	}

	private void add(ProtofieldAccessor unsafeWriter) {
		UnsafeProtofield wrapped = new UnsafeProtofield( unsafeWriter );
		UnsafeProtofield previous = fieldsPerORMName.put( unsafeWriter.getColumnName(), wrapped );
		if ( previous != null ) {
			throw new AssertionFailure( "Duplicate or ambiguous property: '" + unsafeWriter.getColumnName() );
		}
		previous = fieldsPerProtobufName.put( wrapped.getProtobufName(), wrapped );
		if ( previous != null ) {
			throw new AssertionFailure( "Duplicate or ambiguous property after convertion to Protobuf requirements: '"
					+ wrapped.getProtobufName() + "'" );
		}
		orderedFields.add( wrapped );
	}

	public int size() {
		return orderedFields.size();
	}

	public UnsafeProtofield getDecoderByListOrder(int i) {
		return orderedFields.get( i );
	}

	public UnsafeProtofield getDecoderByColumnName(String columnName) {
		return fieldsPerORMName.get( columnName );
	}

	public boolean columnNameExists(String columnName) {
		return fieldsPerORMName.containsKey( columnName );
	}

	public String[] getColumnNames() {
		return fieldsPerORMName.keySet().toArray( new String[0] );
	}

}
