/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.hash;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.impl.RedisJsonBlobType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonByteType;
import org.hibernate.ogm.type.impl.Iso8601StringCalendarType;
import org.hibernate.ogm.type.impl.Iso8601StringDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Container for methods used to obtain the {@link GridType} representation of a {@link Type}.
 *
 * @author Mark Paluch
 */
public class RedisHashTypeConverter {

	public static final RedisHashTypeConverter INSTANCE = new RedisHashTypeConverter();

	private static final Map<Type, GridType> conversionMap = createGridTypeConversionMap();

	private static Map<Type, GridType> createGridTypeConversionMap() {
		Map<Type, GridType> conversion = new HashMap<>();
		conversion.put( StandardBasicTypes.CALENDAR, Iso8601StringCalendarType.DATE_TIME );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, Iso8601StringCalendarType.DATE );
		conversion.put( StandardBasicTypes.DATE, Iso8601StringDateType.DATE );
		conversion.put( StandardBasicTypes.TIME, Iso8601StringDateType.TIME );
		conversion.put( StandardBasicTypes.TIMESTAMP, Iso8601StringDateType.DATE_TIME );
		conversion.put( StandardBasicTypes.BYTE, RedisJsonByteType.INSTANCE );
		conversion.put( StandardBasicTypes.INTEGER, RedisHashType.INTEGER );
		conversion.put( StandardBasicTypes.SHORT, RedisHashType.SHORT );
		conversion.put( StandardBasicTypes.LONG, RedisHashType.LONG );
		conversion.put( StandardBasicTypes.DOUBLE, RedisHashType.DOUBLE );
		conversion.put( StandardBasicTypes.FLOAT, RedisHashType.FLOAT );
		conversion.put( StandardBasicTypes.BYTE, RedisHashType.BYTE );
		conversion.put( BinaryType.INSTANCE, RedisJsonBlobType.INSTANCE );
		conversion.put( MaterializedBlobType.INSTANCE, RedisJsonBlobType.INSTANCE );

		conversion.put( StandardBasicTypes.BOOLEAN, RedisHashType.BOOLEAN );
		conversion.put( StandardBasicTypes.NUMERIC_BOOLEAN, RedisHashType.NUMERIC_BOOLEAN );
		conversion.put( StandardBasicTypes.UUID_BINARY, RedisHashType.UUID_BINARY );
		return conversion;
	}

	/**
	 * Returns the {@link GridType} representing the {@link Type}.
	 *
	 * @param type the Type that needs conversion
	 *
	 * @return the corresponding GridType
	 */
	public GridType convert(Type type) {

		if ( type instanceof CustomType ) {
			CustomType customType = (CustomType) type;
			if ( customType.getUserType() instanceof EnumType ) {
				EnumType enumType = (EnumType) customType.getUserType();
				return ( new RedisHashEnumType( customType, enumType ) );
			}
		}

		return conversionMap.get( type );
	}
}
