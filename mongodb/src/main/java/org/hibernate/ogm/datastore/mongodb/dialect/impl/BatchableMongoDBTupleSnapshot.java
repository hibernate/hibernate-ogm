/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class BatchableMongoDBTupleSnapshot extends MongoDBTupleSnapshot {

	/**
	 * Identify the purpose for the creation of a {@link BatchableMongoDBTupleSnapshot}
	 *
	 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
	 */
	public enum SnapshotType {

		INSERT, UPDATE
	}

	private final SnapshotType operationType;

	public BatchableMongoDBTupleSnapshot(DBObject dbObject, EntityKeyMetadata entityKeyMetadata, SnapshotType operationType) {
		super( dbObject, entityKeyMetadata );
		this.operationType = operationType;
	}

	public SnapshotType getOperationType() {
		return operationType;
	}
}
