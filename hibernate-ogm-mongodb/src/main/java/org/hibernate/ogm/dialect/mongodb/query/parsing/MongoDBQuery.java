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
package org.hibernate.ogm.dialect.mongodb.query.parsing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.BackendQuery;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBTupleSnapshot;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.persister.OgmEntityPersister;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * A {@link BackendQuery} targeting a MongoDB store.
 *
 * @author Gunnar Morling
 */
public class MongoDBQuery implements BackendQuery {

	private final Class<?> entityType;
	private final DBObject query;
	private final DBObject projection;

	public MongoDBQuery(Class<?> entityType, DBObject query, DBObject projection) {
		this.entityType = entityType;
		this.query = query;
		this.projection = projection;
	}

	/**
	 * @return the query
	 */
	public DBObject getQuery() {
		return query;
	}

	/**
	 * @return the entityType
	 */
	@Override
	public Class<?> getEntityType() {
		return entityType;
	}

	/**
	 * @return the projection
	 */
	public DBObject getProjection() {
		return projection;
	}

	@Override
	public boolean isProjection() {
		return !projection.keySet().isEmpty();
	}

	@Override
	public List<String> getProjectionColumns() {
		return new ArrayList<String>( projection.keySet() );
	}

	@Override
	public Iterator<Tuple> execute(OgmSession session, Map<String, Object> namedParameters) {
		EntityKeyMetadata keyMetaData = getKeyMetaData( session, entityType );
		MongoDBDatastoreProvider provider = (MongoDBDatastoreProvider) session.getSessionFactory().getServiceRegistry().getService( DatastoreProvider.class );

		DBCollection collection = provider.getDatabase().getCollection( keyMetaData.getTable() );
		DBCursor cursor = isProjection() ? collection.find( query, projection ) : collection.find( query );

		return new MongoDBResultsCursorIterator( cursor, keyMetaData );
	}

	private EntityKeyMetadata getKeyMetaData(OgmSession session, Class<?> entityType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( session.getFactory() ).getEntityPersister( entityType.getName() );
		return new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
	}

	/**
	 * Wraps a given {@link DBCursor} and exposes its entries as {@link Tuple}s.
	 *
	 * @author Gunnar Morling
	 */
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

			return new Tuple( new MongoDBTupleSnapshot( dbObject, rowKey ) );
		}

		@Override
		public void remove() {
			cursor.remove();
		}
	}

	@Override
	public String toString() {
		return "MongoDBQueryParsingResult [entityType=" + entityType.getSimpleName() + ", query=" + query + ", projection=" + projection + "]";
	}
}
