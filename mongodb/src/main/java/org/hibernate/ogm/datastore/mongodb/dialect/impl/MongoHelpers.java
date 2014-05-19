/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.ogm.grid.AssociationKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoHelpers {

	//only for embedded
	public static Collection<DBObject> getAssociationFieldOrNull(AssociationKey key, DBObject entity) {
		String[] path = key.getCollectionRole().split( "\\." );
		Object field = entity;
		for (String node : path) {
			field = field != null ? ( (DBObject) field).get( node ) : null;
		}
		return (Collection<DBObject>) field;
	}

	public static void addEmptyAssociationField(AssociationKey key, DBObject entity) {
		String[] path = key.getCollectionRole().split( "\\." );
		Object field = entity;
		int size = path.length;
		for (int index = 0 ; index < size ; index++) {
			String node = path[index];
			DBObject parent = (DBObject) field;
			field = parent.get( node );
			if ( field == null ) {
				if ( index == size - 1 ) {
					field = Collections.EMPTY_LIST;
				}
				else {
					field = new BasicDBObject();
				}
				parent.put( node, field );
			}
		}
	}

	// Return null if the column is not present
	public static Object getValueFromColumns(String column, String[] columns, Object[] values) {
		for ( int index = 0 ; index < columns.length ; index++ ) {
			if ( columns[index].equals( column ) ) {
				return values[index];
			}
		}
		return null;
	}
}
