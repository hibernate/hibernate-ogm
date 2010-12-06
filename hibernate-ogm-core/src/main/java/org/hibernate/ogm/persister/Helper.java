package org.hibernate.ogm.persister;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.ogm.type.GridType;

/**
 * @author Emmanuel Bernard
 */
class Helper {
	public static Object[] getColumnValuesFromResultset(Map<String, Object> resultset, String[] propertyColumnNames) {
		Object[] columnValues;
		final int nbrOfColumns = propertyColumnNames.length;
		columnValues = new Object[nbrOfColumns];
		for ( int columnIndex = 0; columnIndex < nbrOfColumns; columnIndex++ ) {
			columnValues[columnIndex] = resultset.get( propertyColumnNames[columnIndex] );
		}
		return columnValues;
	}

	public static Object[] getColumnsValuesFromObjectValue(Object uniqueKey, GridType gridUniqueKeyType, String[] propertyColumnNames, SessionImplementor session) {
		Map<String, Object> tempResultset = new HashMap<String, Object>(2);
		gridUniqueKeyType.nullSafeSet( tempResultset, uniqueKey, propertyColumnNames, session) ;
		return Helper.getColumnValuesFromResultset( tempResultset, propertyColumnNames );
	}
}
