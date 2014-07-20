/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import com.mongodb.DBObject;
import org.hibernate.ogm.grid.EntityKey;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class MongoDBTupleSnapshot extends BasicMongoDBTupleSnapshot {

	/**
	 * Identify the purpose for the creation of a {@link MongoDBTupleSnapshot}
	 *
	 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
	 */
	public enum SnapshotType {

		INSERT, UPDATE
	}

	private final SnapshotType operationType;

	public MongoDBTupleSnapshot(DBObject dbObject, EntityKey key, SnapshotType operationType) {
		super( dbObject, key.getColumnNames() );
		this.operationType = operationType;

	}

	public DBObject getDbObject() {
		return dbObject;
	}

	public SnapshotType getOperationType() {
		return operationType;
	}

}
