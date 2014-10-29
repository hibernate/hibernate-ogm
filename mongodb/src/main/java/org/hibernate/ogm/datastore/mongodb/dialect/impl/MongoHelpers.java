/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.regex.Pattern;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Provides functionality for dealing with (nested) fields of MongoDB documents.
 *
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class MongoHelpers {

	private static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );

	public static void setValue(DBObject entity, String column, Object value) {
		// fast path for non-embedded case
		if ( !column.contains( "." ) ) {
			entity.put( column, value );
		}
		else {
			String[] path = DOT_SEPARATOR_PATTERN.split( column );
			Object field = entity;
			int size = path.length;
			for (int index = 0 ; index < size ; index++) {
				String node = path[index];
				DBObject parent = (DBObject) field;
				field = parent.get( node );
				if ( field == null ) {
					if ( index == size - 1 ) {
						field = value;
					}
					else {
						field = new BasicDBObject();
					}
					parent.put( node, field );
				}
			}
		}
	}

	public static boolean hasField(DBObject entity, String dothPath) {
		return getValueOrNull( entity, dothPath ) != null;
	}

	public static <T> T getValueOrNull(DBObject entity, String dothPath, Class<T> type) {
		Object value = getValueOrNull( entity, dothPath );
		return type.isInstance( value ) ? type.cast( value ) : null;
	}

	public static Object getValueOrNull(DBObject entity, String dotPath) {
		// fast path for simple properties
		if ( !dotPath.contains( "." ) ) {
			return entity.get( dotPath );
		}

		String[] path = DOT_SEPARATOR_PATTERN.split( dotPath );
		int size = path.length;

		for (int index = 0 ; index < size - 1; index++) {
			Object next = entity.get( path[index] );
			if ( next == null || !( next instanceof DBObject ) ) {
				return null;
			}
			entity = (DBObject) next;
		}

		return entity.get( path[size - 1] );
	}
}
