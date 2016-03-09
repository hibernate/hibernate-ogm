/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

		if ( type == StandardBasicTypes.BIG_DECIMAL ) {
			return IgniteBigDecimalType.INSTANCE;
		}

		if ( type == StandardBasicTypes.BIG_INTEGER ) {
			return IgniteBigIntegerType.INSTANCE;
		}

		if ( type == StandardBasicTypes.CALENDAR ) {
			return IgniteCalendarType.INSTANCE;
		}

		if ( type == StandardBasicTypes.CALENDAR_DATE ) {
			return IgniteCalendarType.INSTANCE;
		}

		return null;
	}

}
