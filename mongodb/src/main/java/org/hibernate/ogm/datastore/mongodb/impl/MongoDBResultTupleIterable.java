/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * A wrapper for the {@link DBCursor} used in MongoDB which converts the iterated {@link DBObject}s to corresponding
 * tuples.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class MongoDBResultTupleIterable implements Iterable<Tuple>, Closeable {

	private final DBCursor cursor;
	private final EntityKeyMetadata keyMetaData;

	/**
	 * Creates a new {@code MongoDBResultTupleIterable}.
	 *
	 * @param cursor the DBCursor from which DBObject are retrieved
	 * @param keyMetaData the metadata information of the entity type that is returned by the iterator
	 */
	public MongoDBResultTupleIterable(DBCursor cursor, EntityKeyMetadata keyMetaData) {
		this.cursor = cursor;
		this.keyMetaData = keyMetaData;
	}

	@Override
	public void close() throws IOException {
		cursor.close();
	}

	@Override
	public Iterator<Tuple> iterator() {
		return new MongoDBResultsCursorIterator( cursor, keyMetaData );
	}

	private static class MongoDBResultsCursorIterator implements Iterator<Tuple> {

		private final DBCursor cursor;
		private final EntityKeyMetadata keyMetaData;

		public MongoDBResultsCursorIterator(DBCursor cursor, EntityKeyMetadata keyMetaData) {
			this.cursor = cursor;
			this.keyMetaData = keyMetaData;
		}

		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public Tuple next() {
			DBObject dbObject = cursor.next();

			RowKey rowKey = new RowKey(
					keyMetaData.getTable(),
					keyMetaData.getColumnNames(),
					new Object[] { dbObject.get( MongoDBDialect.ID_FIELDNAME ) }
					);

			return new Tuple( new MongoDBTupleSnapshot( dbObject, rowKey, SnapshotType.SELECT) );
		}

		@Override
		public void remove() {
			cursor.remove();
		}
	}
}
