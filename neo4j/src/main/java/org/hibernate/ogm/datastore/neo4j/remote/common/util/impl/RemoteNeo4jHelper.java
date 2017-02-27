/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.util.impl;

import java.util.Map;

import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * @author Davide D'Alto
 */
public class RemoteNeo4jHelper {

	private RemoteNeo4jHelper() {
	}

	/**
	 * Check if the node matches the column values
	 *
	 * @param nodeProperties the properties on the node
	 * @param keyColumnNames the name of the columns to check
	 * @param keyColumnValues the value of the columns to check
	 * @return true if the properties of the node match the column names and values
	 */
	public static boolean matches(Map<String, Object> nodeProperties, String[] keyColumnNames, Object[] keyColumnValues) {
		for ( int i = 0; i < keyColumnNames.length; i++ ) {
			String property = keyColumnNames[i];
			Object expectedValue = keyColumnValues[i];
			boolean containsProperty = nodeProperties.containsKey( property );
			if ( containsProperty ) {
				Object actualValue = nodeProperties.get( property );
				if ( !sameValue( expectedValue, actualValue ) ) {
					return false;
				}
			}
			else if ( expectedValue != null ) {
				return false;
			}
		}
		return true;
	}

	public static boolean matches(RowKey actual, RowKey expected) {
		if ( actual.getColumnNames().length != expected.getColumnNames().length ) {
			return false;
		}
		for ( int i = 0; i < expected.getColumnNames().length; i++ ) {
			Object expectedValue = expected.getColumnValues()[i];
			Object actualValue = actual.getColumnValues()[i];
			if ( !sameValue( expectedValue, actualValue ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameValue(Object expectedValue, Object actualValue) {
		if ( actualValue == null && expectedValue != null ) {
			return false;
		}
		if ( actualValue != null && !actualValue.equals( expectedValue ) ) {
			// Neo4j remote (bolt) does not save the type of the original value, for example if the original value was
			// a Long smaller than the max integer the query will return the value as integer.
			if ( !(actualValue instanceof Number && actualValue.toString().equals( expectedValue.toString() ) ) ) {
				return false;
			}
		}
		return true;
	}
}
