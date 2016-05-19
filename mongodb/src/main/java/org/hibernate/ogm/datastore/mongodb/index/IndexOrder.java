/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index;

/**
 * A document that contains the field and value pairs where the field is the
 * index key and the value describes the type of index for that field. For an
 * ascending index on a field, specify a value of 1; for descending index,
 * specify a value of -1. cf
 * https://docs.mongodb.org/manual/reference/method/db.collection.createIndex/#
 * db.collection.createIndex
 *
 * XXX Fix Javadoc
 */
public enum IndexOrder {

	ASCENDING(1), DESCENDING(-1);

	private int indexKeyValue;

	IndexOrder(int indexKeyValue) {
		this.indexKeyValue = indexKeyValue;
	}

	public int getIndexKeyValue() {
		return indexKeyValue;
	}
}
