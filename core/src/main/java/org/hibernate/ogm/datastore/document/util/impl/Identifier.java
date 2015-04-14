/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.util.impl;

import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.internal.util.StringHelper.suffix;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Generates the ids used for document stores CouchDB and CouchBase
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Ewa Stawicka &lt;ewastawicka91@gmail.com&gt;
 */
public class Identifier {

	private static final String COLUMN_VALUES_SEPARATOR = "_";
	private static final String EMPTY_STRING = "";

	/**
	 * Create the id used to document store CouchDB and CouchBase.
	 *
	 * @param key the {@link EntityKey} used to generate the id
	 * @return the value of the generate id
	 */
	public static String createEntityId(EntityKey key) {
		return createId( key.getTable(), key.getColumnNames(), key.getColumnValues(), COLUMN_VALUES_SEPARATOR, true );
	}

	/**
	 * Create the id for association used to document store CouchDB and CouchBase.
	 *
	 * @param key the {@link AssociationKey} used to generate the id
	 * @return the value of the generate id
	 */
	public static String createAssociationId(AssociationKey key) {
		return createId( key.getTable(), key.getColumnNames(), key.getColumnValues(), COLUMN_VALUES_SEPARATOR, true );
	}

	/**
	 * Create the id for sequence used to document store CouchDB and CouchBase.
	 *
	 * @param key the {@link IdSourceKey} used to generate the id
	 * @return the value of the generate id
	 */
	public static String createSourceId(IdSourceKey key) {
		return createId( key.getTable(), key.getColumnNames(), key.getColumnValues(), EMPTY_STRING, false );
	}

	private static String createId(String tableName, String[] columnNames, Object[] columnValues, String separator, boolean escape) {
		String[] stringColumnValues = toStringArray( columnValues );
		if ( escape ) {
			columnNames = escape( columnNames );
			stringColumnValues = escape( stringColumnValues );
		}

		StringBuilder sb = new StringBuilder( tableName );
		sb.append( ":" );
		sb.append( join( EMPTY_STRING, suffix( columnNames, separator)) );
		sb.append( ":" );
		sb.append( join( EMPTY_STRING, suffix( stringColumnValues, separator ))) ;
		return sb.toString();
	}

	private static String[] escape(String[] colVal) {
		return replace( colVal, "_", "/_" );
	}
}
