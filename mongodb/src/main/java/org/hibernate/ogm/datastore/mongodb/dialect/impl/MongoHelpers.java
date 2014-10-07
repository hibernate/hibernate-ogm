/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.Collections;
import java.util.regex.Pattern;

import org.hibernate.ogm.model.key.spi.AssociationKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class MongoHelpers {

	public static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );

	public static void addEmptyAssociationField(AssociationKey key, DBObject entity) {
		String column = key.getMetadata().getCollectionRole();
		Object value = Collections.EMPTY_LIST;
		setValue( entity, column, value );
	}

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
}
