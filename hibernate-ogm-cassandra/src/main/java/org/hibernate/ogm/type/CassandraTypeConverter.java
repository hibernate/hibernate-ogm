/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 *  as indicated by the @authors tag. All rights reserved.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This copyrighted material is made available to anyone wishing to use,
 *  modify, copy, or redistribute it subject to the terms and conditions
 *  of the GNU Lesser General Public License, v. 2.1.
 *  This program is distributed in the hope that it will be useful, but WITHOUT A
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License,
 *  v.2.1 along with this distribution; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA  02110-1301, USA.
 */

package org.hibernate.ogm.type;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import org.apache.commons.codec.binary.Hex;

import org.hibernate.ogm.type.cassandra.BigDecimalType;
import org.hibernate.ogm.type.cassandra.BigIntegerType;
import org.hibernate.ogm.type.cassandra.BlobType;
import org.hibernate.ogm.type.cassandra.ByteType;
import org.hibernate.ogm.type.cassandra.IntegerType;
import org.hibernate.ogm.type.cassandra.LongType;
import org.hibernate.ogm.type.cassandra.StringCalendarType;
import org.hibernate.ogm.type.cassandra.StringDateType;
import org.hibernate.ogm.type.cassandra.StringType;
import org.hibernate.ogm.type.cassandra.UUIDType;
import org.hibernate.ogm.type.cassandra.UrlType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * @author Khanh Tuong Maudoux
 */
public class CassandraTypeConverter {

	public static final CassandraTypeConverter INSTANCE = new CassandraTypeConverter();

	private static final Map<Type, GridType> overrideTypes = createGridTypeConversionMap();

	private static Map<Type, GridType> createGridTypeConversionMap() {
		Map<Type, GridType> conversion = new HashMap<Type, GridType>();
		conversion.put( StandardBasicTypes.UUID_CHAR, UUIDType.INSTANCE );
		conversion.put( StandardBasicTypes.UUID_BINARY, UUIDType.INSTANCE );
		conversion.put( StandardBasicTypes.BLOB, BlobType.INSTANCE );
		conversion.put( StandardBasicTypes.STRING, StringType.INSTANCE );
		conversion.put( StandardBasicTypes.INTEGER, IntegerType.INSTANCE );
		conversion.put( StandardBasicTypes.BIG_INTEGER, BigIntegerType.INSTANCE );
		conversion.put( StandardBasicTypes.BIG_DECIMAL, BigDecimalType.INSTANCE );
		conversion.put( StandardBasicTypes.MATERIALIZED_BLOB, BlobType.INSTANCE );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, StringCalendarType.INSTANCE );
		conversion.put( StandardBasicTypes.CALENDAR, StringCalendarType.INSTANCE );
		conversion.put( StandardBasicTypes.TIMESTAMP, StringDateType.INSTANCE );
		conversion.put( StandardBasicTypes.DATE, StringDateType.INSTANCE );
		conversion.put( StandardBasicTypes.TIME, StringDateType.INSTANCE );
		conversion.put( StandardBasicTypes.URL, UrlType.INSTANCE );
		conversion.put( StandardBasicTypes.BYTE, ByteType.INSTANCE );
		conversion.put( StandardBasicTypes.LONG, LongType.INSTANCE );
		conversion.put( StandardBasicTypes.BOOLEAN, BooleanType.INSTANCE );
		return conversion;
	}

	public GridType convert(Type type) {
		GridType gridType = overrideTypes.get( type );
		return gridType;
	}

	public static Object getValue(Row row, String columnName, DataType dataType) {
		switch ( dataType.getName() ) {
			case ASCII:
				return row.getString( columnName );
			case TEXT:
				return row.getString( columnName );
			case INT:
				return row.getInt( columnName );
			case BLOB:
				//TODO : for blob type, data are marshalled with hexa
				ByteBuffer byteBuffer = row.getBytes( columnName );

				if ( byteBuffer != null ) {
					byte[] b = new byte[byteBuffer.remaining()];
					byteBuffer.get( b );

					String string = new String( Hex.encodeHex( b ) );

					byte[] bytes = new byte[string.length() / 2];
					for ( int i = 0; i < bytes.length; i++ ) {
						final String hexStr = string.substring( i * 2, (i + 1) * 2 );
						bytes[i] = (byte) (Integer.parseInt( hexStr, 16 ) + Byte.MIN_VALUE);
					}
					return bytes;
				}
				return null;
			case UUID:
				return row.getUUID( columnName );
			case TIMESTAMP:
				return row.getDate( columnName );
			case VARINT:
				BigInteger result = row.getVarint( columnName );
				return (result == null) ? 0 : result.byteValue();
			case BOOLEAN:
				return row.getBool( columnName );
			case DECIMAL:
				return row.getDecimal( columnName );
			case BIGINT:
				return BigInteger.valueOf( row.getLong( columnName ) );
			// TODO : conflict between Long (ex: userId into test type) and biginteger (ex: visits_count into test type)
			case DOUBLE:
				return row.getDouble( columnName );
			default:
				return null;
		}
	}
}
