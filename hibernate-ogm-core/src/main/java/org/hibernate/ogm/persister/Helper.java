package org.hibernate.ogm.persister;

import java.util.Map;

import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Emmanuel Bernard
 */
class Helper {
	public static Object[] getColumnValuesFromResultset(Map<String, Object> resultset, int index, OgmEntityPersister persister) {
		Object[] columnValues;
		final String[] propertyColumnNames = persister.getPropertyColumnNames( index );
		final int nbrOfColumns = propertyColumnNames.length;
		columnValues = new Object[nbrOfColumns];
		for ( int columnIndex = 0; columnIndex < nbrOfColumns; columnIndex++ ) {
			columnValues[columnIndex] = resultset.get( propertyColumnNames[columnIndex] );
		}
		return columnValues;
	}
}
