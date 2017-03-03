/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.regex.Pattern;

import org.bson.Document;

/**
 * Provides functionality for dealing with (nested) fields of MongoDB documents.
 *
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class MongoHelpers {

	private static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );

	public static void setValue(Document entity, String column, Object value) {
		// fast path for non-embedded case
		if ( !column.contains( "." ) ) {
			entity.put( column, value );
		}
		else {
			String[] path = DOT_SEPARATOR_PATTERN.split( column );
			Object field = entity;
			int size = path.length;
			for ( int index = 0; index < size; index++ ) {
				String node = path[index];
				Document parent = (Document) field;
				field = parent.get( node );
				if ( field == null ) {
					if ( index == size - 1 ) {
						field = value;
					}
					else {
						field = new Document();
					}
					parent.put( node, field );
				}
			}
		}
	}

	/**
	 * Remove a column from the Document
	 *
	 * @param entity the {@link Document} with the column
	 * @param column the column to remove
	 */
	public static void resetValue(Document entity, String column) {
		// fast path for non-embedded case
		if ( !column.contains( "." ) ) {
			entity.remove( column );
		}
		else {
			String[] path = DOT_SEPARATOR_PATTERN.split( column );
			Object field = entity;
			int size = path.length;
			for ( int index = 0; index < size; index++ ) {
				String node = path[index];
				Document parent = (Document) field;
				field = parent.get( node );
				if ( field == null && index < size - 1 ) {
					//TODO clean up the hierarchy of empty containers
					// no way to reach the leaf, nothing to do
					return;
				}
				if ( index == size - 1 ) {
					parent.remove( node );
				}
			}
		}
	}

	public static boolean hasField(Document entity, String dotPath) {
		return getValueOrNull( entity, dotPath ) != null;
	}

	public static <T> T getValueOrNull(Document entity, String dotPath, Class<T> type) {
		Object value = getValueOrNull( entity, dotPath );
		return type.isInstance( value ) ? type.cast( value ) : null;
	}

	public static Object getValueOrNull(Document entity, String dotPath) {
		// fast path for simple properties
		if ( !dotPath.contains( "." ) ) {
			return entity.get( dotPath );
		}

		String[] path = DOT_SEPARATOR_PATTERN.split( dotPath );
		int size = path.length;

		for ( int index = 0; index < size - 1; index++ ) {
			Object next = entity.get( path[index] );
			if ( next == null || !( next instanceof Document ) ) {
				return null;
			}
			entity = (Document) next;
		}

		String field = path[size - 1];
		return entity.get( field );
	}

	/**
	 * Links the two field names into a single left.right field name.
	 * If the left field is empty, right is returned
	 *
	 * @param left one field name
	 * @param right the other field name
	 * @return left.right or right if left is an empty string
	 */
	public static String flatten(String left, String right) {
		return left == null || left.isEmpty() ? right : left + "." + right;
	}
}
