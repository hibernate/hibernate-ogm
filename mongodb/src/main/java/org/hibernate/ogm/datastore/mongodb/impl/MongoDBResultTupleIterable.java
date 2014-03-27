/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.TupleIterator;
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
public class MongoDBResultTupleIterable implements Iterable<Tuple> {

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
	public TupleIterator iterator() {
		return new MongoDBResultsCursorIterator( cursor, keyMetaData );
	}

	private static class MongoDBResultsCursorIterator implements TupleIterator {

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

		@Override
		public void close() {
			cursor.close();
		}
	}
}
