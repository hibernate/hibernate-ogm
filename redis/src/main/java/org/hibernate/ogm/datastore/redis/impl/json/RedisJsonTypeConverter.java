package org.hibernate.ogm.datastore.redis.impl.json;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.impl.RedisJsonBlobType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonByteType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonDoubleType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonIntegerType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonLongType;
import org.hibernate.ogm.type.impl.Iso8601StringCalendarType;
import org.hibernate.ogm.type.impl.Iso8601StringDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Container for methods used to obtain the {@link GridType} representation of a {@link Type}.
 *
 * @author Mark Paluch
 */
public class RedisJsonTypeConverter {

	public static final RedisJsonTypeConverter INSTANCE = new RedisJsonTypeConverter();

	private static final Map<Type, GridType> conversionMap = createGridTypeConversionMap();

	private static Map<Type, GridType> createGridTypeConversionMap() {
		Map<Type, GridType> conversion = new HashMap<Type, GridType>();
		conversion.put( StandardBasicTypes.CALENDAR, Iso8601StringCalendarType.DATE_TIME );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, Iso8601StringCalendarType.DATE );
		conversion.put( StandardBasicTypes.DATE, Iso8601StringDateType.DATE );
		conversion.put( StandardBasicTypes.TIME, Iso8601StringDateType.TIME );
		conversion.put( StandardBasicTypes.TIMESTAMP, Iso8601StringDateType.DATE_TIME );
		conversion.put( StandardBasicTypes.BYTE, RedisJsonByteType.INSTANCE );
		conversion.put( StandardBasicTypes.INTEGER, RedisJsonIntegerType.INSTANCE );
		conversion.put( StandardBasicTypes.LONG, RedisJsonLongType.INSTANCE );
		conversion.put( StandardBasicTypes.DOUBLE, RedisJsonDoubleType.INSTANCE );
		conversion.put( BinaryType.INSTANCE, RedisJsonBlobType.INSTANCE );
		conversion.put( MaterializedBlobType.INSTANCE, RedisJsonBlobType.INSTANCE );
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
		return conversionMap.get( type );
	}

}