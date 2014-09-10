/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;


import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;

/**
 * Helper methods to convert an object value into its column values
 *
 * @author Emmanuel Bernard
 */
public class LogicalPhysicalConverterHelper {
	public static Object[] getColumnValuesFromResultset(Tuple resultset, String[] propertyColumnNames) {
		Object[] columnValues;
		final int nbrOfColumns = propertyColumnNames.length;
		columnValues = new Object[nbrOfColumns];
		for ( int columnIndex = 0; columnIndex < nbrOfColumns; columnIndex++ ) {
			columnValues[columnIndex] = resultset.get( propertyColumnNames[columnIndex] );
		}
		return columnValues;
	}

	public static Object[] getColumnsValuesFromObjectValue(Object uniqueKey, GridType gridUniqueKeyType, String[] propertyColumnNames, SessionImplementor session) {
		Tuple tempResultset = new Tuple();
		gridUniqueKeyType.nullSafeSet( tempResultset, uniqueKey, propertyColumnNames, session) ;
		Object[] columnValuesFromResultset = LogicalPhysicalConverterHelper.getColumnValuesFromResultset( tempResultset, propertyColumnNames );
		return columnValuesFromResultset;
	}
}
