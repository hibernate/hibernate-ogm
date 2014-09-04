/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.type.impl.StringCalendarDateType;
import org.hibernate.ogm.type.impl.StringDateTypeDescriptor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Container for methods used to obtain the {@link GridType} representation of a {@link Type}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jTypeConverter {

	public static final Neo4jTypeConverter INSTANCE = new Neo4jTypeConverter();

	private static final Map<Type, GridType> conversionMap = createGridTypeConversionMap();

	private static Map<Type, GridType> createGridTypeConversionMap() {
		Map<Type, GridType> conversion = new HashMap<Type, GridType>();
		conversion.put( StandardBasicTypes.CALENDAR, StringCalendarDateType.INSTANCE );
		conversion.put( StandardBasicTypes.CALENDAR_DATE, StringCalendarDateType.INSTANCE );
		conversion.put( StandardBasicTypes.DATE, StringDateTypeDescriptor.INSTANCE );
		conversion.put( StandardBasicTypes.TIME, StringDateTypeDescriptor.INSTANCE );
		conversion.put( StandardBasicTypes.TIMESTAMP, StringDateTypeDescriptor.INSTANCE );
		return conversion;
	}

	/**
	 * Returns the {@link GridType} representing the {@link Type}.
	 *
	 * @param type the Type that needs conversion
	 * @return the corresponding GridType
	 */
	public GridType convert(Type type) {
		return conversionMap.get( type );
	}

}
