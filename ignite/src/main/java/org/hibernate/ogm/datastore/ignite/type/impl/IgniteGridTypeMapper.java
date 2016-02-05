package org.hibernate.ogm.datastore.ignite.type.impl;

import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Mapper for Hibernate types, that does not supported by Ignite DataGrid 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteGridTypeMapper {

	public static final IgniteGridTypeMapper INSTANCE = new IgniteGridTypeMapper();
	
	public GridType overrideType(Type type) {

		if ( type == StandardBasicTypes.CALENDAR ) {
			return IgniteCalendarType.INSTANCE;
		}

		if ( type == StandardBasicTypes.CALENDAR_DATE ) {
			return IgniteCalendarType.INSTANCE;
		}

		return null;
	}
	
}
