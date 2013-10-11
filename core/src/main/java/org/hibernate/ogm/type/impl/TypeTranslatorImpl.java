/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.type.BigDecimalType;
import org.hibernate.ogm.type.BigIntegerType;
import org.hibernate.ogm.type.BooleanType;
import org.hibernate.ogm.type.ByteType;
import org.hibernate.ogm.type.CalendarDateType;
import org.hibernate.ogm.type.CalendarType;
import org.hibernate.ogm.type.ClassType;
import org.hibernate.ogm.type.CollectionType;
import org.hibernate.ogm.type.ComponentType;
import org.hibernate.ogm.type.DateType;
import org.hibernate.ogm.type.DoubleType;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.IntegerType;
import org.hibernate.ogm.type.LongType;
import org.hibernate.ogm.type.ManyToOneType;
import org.hibernate.ogm.type.OneToOneType;
import org.hibernate.ogm.type.PrimitiveByteArrayType;
import org.hibernate.ogm.type.StringType;
import org.hibernate.ogm.type.TimeType;
import org.hibernate.ogm.type.TimestampType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.type.UUIDType;
import org.hibernate.ogm.type.UrlType;
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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard
 * @author Nicolas Helleringer
 */
public class TypeTranslatorImpl implements TypeTranslator {
	private final Map<JavaTypeDescriptor, GridType> typeConverter;
	private final GridDialect dialect;

	public TypeTranslatorImpl(GridDialect dialect) {
		this.dialect = dialect;

		typeConverter = new HashMap<JavaTypeDescriptor, GridType>();
		typeConverter.put( ClassTypeDescriptor.INSTANCE, ClassType.INSTANCE );
		typeConverter.put( LongTypeDescriptor.INSTANCE, LongType.INSTANCE );
		typeConverter.put( IntegerTypeDescriptor.INSTANCE, IntegerType.INSTANCE );
		typeConverter.put( DoubleTypeDescriptor.INSTANCE, DoubleType.INSTANCE );
		typeConverter.put( StringTypeDescriptor.INSTANCE, StringType.INSTANCE );
		typeConverter.put( UrlTypeDescriptor.INSTANCE, UrlType.INSTANCE );
		typeConverter.put( BigDecimalTypeDescriptor.INSTANCE, BigDecimalType.INSTANCE );
		typeConverter.put( BigIntegerTypeDescriptor.INSTANCE, BigIntegerType.INSTANCE );
		typeConverter.put( BooleanTypeDescriptor.INSTANCE, BooleanType.INSTANCE );
		typeConverter.put( ByteTypeDescriptor.INSTANCE, ByteType.INSTANCE );
		typeConverter.put( JdbcDateTypeDescriptor.INSTANCE, DateType.INSTANCE );
		typeConverter.put( JdbcTimestampTypeDescriptor.INSTANCE, TimestampType.INSTANCE );
		typeConverter.put( JdbcTimeTypeDescriptor.INSTANCE, TimeType.INSTANCE );
		typeConverter.put( CalendarDateTypeDescriptor.INSTANCE, CalendarDateType.INSTANCE );
		typeConverter.put( CalendarTypeDescriptor.INSTANCE, CalendarType.INSTANCE );
		typeConverter.put( PrimitiveByteArrayTypeDescriptor.INSTANCE, PrimitiveByteArrayType.INSTANCE );
		typeConverter.put( UUIDTypeDescriptor.INSTANCE, UUIDType.INSTANCE );
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
			AbstractStandardBasicType exposedType = (AbstractStandardBasicType) type;
			final GridType gridType = typeConverter.get( exposedType.getJavaTypeDescriptor() );
			if (gridType == null) {
				throw new HibernateException( "Unable to find a GridType for " + exposedType.getClass().getName() );
			}
			return gridType;
		}
		else if ( type instanceof CustomType ) {
			CustomType cType = (CustomType) type;
			final UserType userType = cType.getUserType();
			if ( userType instanceof EnumType ) {
				EnumType enumType = (EnumType) userType;
				//should we cache that (the key must be enumClass / isOrdinal
				return new org.hibernate.ogm.type.EnumType( cType, enumType );
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
		throw new HibernateException( "Unable to find a GridType for " + type.getClass().getName() );
	}
}
