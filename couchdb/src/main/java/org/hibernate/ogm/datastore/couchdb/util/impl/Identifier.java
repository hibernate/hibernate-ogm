/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.util.impl;

import java.util.regex.Pattern;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Generates the ids used to create the {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document}
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class Identifier {

	private static final String COLUMN_VALUES_SEPARATOR = "_";
	private static final Pattern escapingPattern = Pattern.compile( COLUMN_VALUES_SEPARATOR );

	/**
	 * Create the id used to store an {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument}
	 *
	 * @param key the {@link EntityKey} used to generate the id
	 * @return the value of the generate id
	 */
	public static String createEntityId(EntityKey key) {
		return key.getTable() + ":" + fromColumnValues( key.getColumnNames() ) + ":" + fromColumnValues( key.getColumnValues() );
	}

	/**
	 * Create the id used to store an {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument}
	 *
	 * @param key the{@link AssociationKey} used to generate the id
	 * @return the value of the generate id
	 */
	public static String createAssociationId(AssociationKey key) {
		return key.getTable() + ":" + fromColumnValues( key.getColumnNames() ) + ":" + fromColumnValues( key.getColumnValues() );
	}

	private static String fromColumnValues(Object[] columnValues) {
		String id = "";
		for ( int i = 0; i < columnValues.length; i++ ) {
			id += escapeCharsValuesUsedAsColumnValuesSeparator( columnValues[i] ) + COLUMN_VALUES_SEPARATOR;
		}
		return id;
	}

	private static String escapeCharsValuesUsedAsColumnValuesSeparator(Object columnValue) {
		final String value = String.valueOf( columnValue );
		return escapingPattern.matcher( value ).replaceAll( "/_" );
	}
}
