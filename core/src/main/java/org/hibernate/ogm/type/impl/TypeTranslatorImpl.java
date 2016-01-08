/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Collections;
import java.util.Map;

import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.type.descriptor.impl.AttributeConverterGridTypeDescriptorAdaptor;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.usertype.UserType;

/**
 * @author Emmanuel Bernard
 * @author Nicolas Helleringer
 */
public class TypeTranslatorImpl implements TypeTranslator {

	private static final Log log = LoggerFactory.make();

	// ORM Type to OGM GridType relation
	private final Map<Type, GridType> typeConverter;
	private final GridDialect dialect;
	// ORM type resolver
	private final TypeResolver typeResolver;

	public TypeTranslatorImpl(GridDialect dialect, TypeResolver typeResolver) {
		this.dialect = dialect;
		this.typeResolver = typeResolver;

		Map<Type, GridType> tmpMap = newHashMap( 20 );
		tmpMap.put( org.hibernate.type.ClassType.INSTANCE, ClassType.INSTANCE );
		tmpMap.put( org.hibernate.type.LongType.INSTANCE, LongType.INSTANCE );
		tmpMap.put( org.hibernate.type.IntegerType.INSTANCE, IntegerType.INSTANCE );
		tmpMap.put( org.hibernate.type.DoubleType.INSTANCE, DoubleType.INSTANCE );
		tmpMap.put( org.hibernate.type.FloatType.INSTANCE, FloatType.INSTANCE );
		tmpMap.put( org.hibernate.type.ShortType.INSTANCE, ShortType.INSTANCE );
		tmpMap.put( org.hibernate.type.CharacterType.INSTANCE, CharacterType.INSTANCE );
		tmpMap.put( org.hibernate.type.StringType.INSTANCE, StringType.INSTANCE );
		tmpMap.put( org.hibernate.type.UrlType.INSTANCE, UrlType.INSTANCE );
		tmpMap.put( org.hibernate.type.BigDecimalType.INSTANCE, BigDecimalType.INSTANCE );
		tmpMap.put( org.hibernate.type.BigIntegerType.INSTANCE, BigIntegerType.INSTANCE );
		tmpMap.put( org.hibernate.type.BooleanType.INSTANCE, BooleanType.INSTANCE );
		tmpMap.put( org.hibernate.type.TrueFalseType.INSTANCE, TrueFalseType.INSTANCE );
		tmpMap.put( org.hibernate.type.YesNoType.INSTANCE, YesNoType.INSTANCE );
		tmpMap.put( org.hibernate.type.NumericBooleanType.INSTANCE, NumericBooleanType.INSTANCE );
		tmpMap.put( org.hibernate.type.ByteType.INSTANCE, ByteType.INSTANCE );
		tmpMap.put( org.hibernate.type.DateType.INSTANCE, DateType.INSTANCE );
		tmpMap.put( org.hibernate.type.TimestampType.INSTANCE, TimestampType.INSTANCE );
		tmpMap.put( org.hibernate.type.TimeType.INSTANCE, TimeType.INSTANCE );
		tmpMap.put( org.hibernate.type.CalendarDateType.INSTANCE, CalendarDateType.INSTANCE );
		tmpMap.put( org.hibernate.type.CalendarType.INSTANCE, CalendarType.INSTANCE );
		tmpMap.put( org.hibernate.type.BinaryType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( org.hibernate.type.MaterializedBlobType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( org.hibernate.type.MaterializedClobType.INSTANCE, StringType.INSTANCE );
		tmpMap.put( org.hibernate.type.ImageType.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( org.hibernate.type.UUIDBinaryType.INSTANCE, UUIDType.INSTANCE );
		tmpMap.put( org.hibernate.type.UUIDCharType.INSTANCE, UUIDType.INSTANCE );

		typeConverter = Collections.unmodifiableMap( tmpMap );
	}

	@Override
	public GridType getType(Type type) {
		if ( type == null ) {
			return null;
		}

		//TODO should we cache results? It seems an actual HashMap might be slower but it makes it more robust
		//     against badly written dialects
		GridType dialectType = dialect.overrideType( type );
		if ( dialectType != null ) {
			return dialectType;
		}
		else if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new SerializableAsByteArrayType<>( exposedType.getJavaTypeDescriptor() );
		}
		else if ( type instanceof AttributeConverterTypeAdapter<?> ) {
			// Handles JPA AttributeConverter integration logic
			return buildAttributeConverterGridTypeAdaptor( (AttributeConverterTypeAdapter<?>) type );

		}
		else if ( type instanceof AbstractStandardBasicType ) {
			AbstractStandardBasicType<?> exposedType = (AbstractStandardBasicType<?>) type;
			final GridType gridType = typeConverter.get( exposedType );
			if (gridType == null) {
				throw log.unableToFindGridType( exposedType.getName() );
			}
			return gridType;
		}
		else if ( type instanceof CustomType ) {
			CustomType cType = (CustomType) type;
			final UserType userType = cType.getUserType();
			if ( userType instanceof EnumType ) {
				EnumType enumType = (EnumType) userType;
				//should we cache that (the key must be enumClass / isOrdinal
				return new org.hibernate.ogm.type.impl.EnumType( cType, enumType );
			}
			//let it go it will eventually fail
		}
		else if ( type instanceof org.hibernate.type.ComponentType ) {
			org.hibernate.type.ComponentType componentType = (org.hibernate.type.ComponentType) type;
			return new ComponentType(componentType, this);
		}
		else if ( type instanceof org.hibernate.type.ManyToOneType ) {
			//do some stuff
			org.hibernate.type.ManyToOneType manyToOneType = (org.hibernate.type.ManyToOneType) type;
			return new ManyToOneType(manyToOneType, this);
		}
		else if ( type instanceof org.hibernate.type.OneToOneType ) {
			//do some stuff
			org.hibernate.type.OneToOneType oneToOneType = (org.hibernate.type.OneToOneType) type;
			return new OneToOneType(oneToOneType, this);
		}
		else if ( type instanceof org.hibernate.type.CollectionType ) {
			return new CollectionType( (org.hibernate.type.CollectionType) type );
		}
		throw log.unableToFindGridType( type.getClass().getName() );
	}

	/**
	 * Logic modeled after {@link SimpleValue#buildAttributeConverterTypeAdapter}
	 * <p>
	 * Adapt AttributeConverter to GridType. Most of the logic is done by the
	 * AttributeConverterGridTypeDescriptorAdaptor class which will call the attribute converter and then call the
	 * GridType compliant with the intermediary type
	 */
	private <T> AttributeConverterGridTypeAdaptor<T> buildAttributeConverterGridTypeAdaptor(AttributeConverterTypeAdapter<T> specificType) {
		// Rebuild the definition as we need some generic type extraction logic from it
		AttributeConverterDefinition attributeConverterDefinition = new AttributeConverterDefinition( specificType.getAttributeConverter(), false );
		final Class<?> databaseColumnJavaType = attributeConverterDefinition.getDatabaseColumnType();

		// Find the GridType for the intermediary datastore Java type (from the attribute converter
		Type intermediaryORMType = typeResolver.basic( databaseColumnJavaType.getName() );
		if ( intermediaryORMType == null ) {
			throw log.cannotFindTypeForAttributeConverter( specificType.getAttributeConverter().getClass(), databaseColumnJavaType );
		}
		GridType intermediaryOGMGridType = this.getType( intermediaryORMType );

		// find the JavaTypeDescriptor representing the "intermediate database type representation".
		final JavaTypeDescriptor<?> intermediateJavaTypeDescriptor = JavaTypeDescriptorRegistry.INSTANCE.getDescriptor( databaseColumnJavaType );
		// and finally construct the adapter, which injects the AttributeConverter calls into the binding/extraction
		// 		process...
		final GridTypeDescriptor gridTypeDescriptorAdapter = new AttributeConverterGridTypeDescriptorAdaptor(
				attributeConverterDefinition.getAttributeConverter(),
				intermediaryOGMGridType,
				intermediateJavaTypeDescriptor
		);
		return new AttributeConverterGridTypeAdaptor<T>(specificType, gridTypeDescriptorAdapter);
	}
}
