/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.json;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.impl.RedisJsonBlobType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonByteType;
import org.hibernate.ogm.datastore.redis.impl.RedisJsonLongType;
import org.hibernate.ogm.datastore.redis.impl.RedisSerializableType;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.ogm.type.impl.Iso8601StringCalendarType;
import org.hibernate.ogm.type.impl.Iso8601StringDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Container for methods used to obtain the {@link GridType} representation of a {@link Type}.
 *
 * @author Mark Paluch
 */
public class RedisJsonTypeConverter {

	public static final RedisJsonTypeConverter INSTANCE = new RedisJsonTypeConverter();

	private static final Map<Type, AbstractGenericBasicType<?>> conversionMap = createGridTypeConversionMap();

	private static Map<Type, AbstractGenericBasicType<?>> createGridTypeConversionMap() {
		Map<Type, AbstractGenericBasicType<? extends Object>> conversion = new HashMap<Type, AbstractGenericBasicType<? extends Object>>();
		conversion.put( StandardBasicTypes.CALENDAR, Iso8601StringCalendarType.DATE_TIME );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, Iso8601StringCalendarType.DATE );
		conversion.put( StandardBasicTypes.DATE, Iso8601StringDateType.DATE );
		conversion.put( StandardBasicTypes.TIME, Iso8601StringDateType.TIME );
		conversion.put( StandardBasicTypes.TIMESTAMP, Iso8601StringDateType.DATE_TIME );
		conversion.put( StandardBasicTypes.BYTE, RedisJsonByteType.INSTANCE );
		conversion.put( StandardBasicTypes.LONG, RedisJsonLongType.INSTANCE );
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
		if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new RedisSerializableType<>( exposedType.getJavaTypeDescriptor() );
		}

		return (AbstractGenericBasicType<Object>) conversionMap.get( type );
	}
}
