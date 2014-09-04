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

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimeTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.hibernate.usertype.UserType;

/**
 * @author Emmanuel Bernard
 * @author Nicolas Helleringer
 */
public class TypeTranslatorImpl implements TypeTranslator {

	private static final Log log = LoggerFactory.make();

	private final Map<JavaTypeDescriptor<?>, GridType> typeConverter;
	private final GridDialect dialect;

	public TypeTranslatorImpl(GridDialect dialect) {
		this.dialect = dialect;

		Map<JavaTypeDescriptor<?>, GridType> tmpMap = newHashMap( 17 );
		tmpMap.put( ClassTypeDescriptor.INSTANCE, ClassType.INSTANCE );
		tmpMap.put( LongTypeDescriptor.INSTANCE, LongType.INSTANCE );
		tmpMap.put( IntegerTypeDescriptor.INSTANCE, IntegerType.INSTANCE );
		tmpMap.put( DoubleTypeDescriptor.INSTANCE, DoubleType.INSTANCE );
		tmpMap.put( StringTypeDescriptor.INSTANCE, StringType.INSTANCE );
		tmpMap.put( UrlTypeDescriptor.INSTANCE, UrlType.INSTANCE );
		tmpMap.put( BigDecimalTypeDescriptor.INSTANCE, BigDecimalType.INSTANCE );
		tmpMap.put( BigIntegerTypeDescriptor.INSTANCE, BigIntegerType.INSTANCE );
		tmpMap.put( BooleanTypeDescriptor.INSTANCE, BooleanType.INSTANCE );
		tmpMap.put( ByteTypeDescriptor.INSTANCE, ByteType.INSTANCE );
		tmpMap.put( JdbcDateTypeDescriptor.INSTANCE, DateType.INSTANCE );
		tmpMap.put( JdbcTimestampTypeDescriptor.INSTANCE, TimestampType.INSTANCE );
		tmpMap.put( JdbcTimeTypeDescriptor.INSTANCE, TimeType.INSTANCE );
		tmpMap.put( CalendarDateTypeDescriptor.INSTANCE, CalendarDateType.INSTANCE );
		tmpMap.put( CalendarTypeDescriptor.INSTANCE, CalendarType.INSTANCE );
		tmpMap.put( PrimitiveByteArrayTypeDescriptor.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		tmpMap.put( UUIDTypeDescriptor.INSTANCE, UUIDType.INSTANCE );

		typeConverter = Collections.unmodifiableMap( tmpMap );
	}

	@Override public GridType getType(Type type) {
		if ( type == null ) {
			return null;
		}

		//TODO should we cache results? It seems an actual HashMap might be slower but it makes it more robust
		//     against badly written dialects
		GridType dialectType = dialect.overrideType( type );
		if ( dialectType != null ) {
			return dialectType;
		}
		else if ( type instanceof AbstractStandardBasicType ) {
			AbstractStandardBasicType<?> exposedType = (AbstractStandardBasicType<?>) type;
			final GridType gridType = typeConverter.get( exposedType.getJavaTypeDescriptor() );
			if (gridType == null) {
				throw log.unableToFindGridType( exposedType.getJavaTypeDescriptor().getJavaTypeClass().getName() );
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
}
