/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 */
public class MongoDBAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, DBObject> map;
	private final DBObject assoc;

	public MongoDBAssociationSnapshot(DBObject assoc) {
		this.assoc = assoc;
		this.map = new LinkedHashMap<RowKey, DBObject>();
		
		for ( DBObject row : getRows() ) {
			List<String> columnNames = new ArrayList<String>();
			List<Object> columnValues = new ArrayList<Object>();
			DBObject columns = (DBObject)row.get( MongoDBDialect.COLUMNS_FIELDNAME );
			
			for ( String columnKey : columns.keySet() ) {
				columnNames.add( columnKey );
				columnValues.add( columns.get( columnKey ) );
			}
			
			RowKey rowKey = new RowKey( (String)row.get( MongoDBDialect.TABLE_FIELDNAME ),
					columnNames.toArray( new String[]{} ),
					columnValues.toArray() );
			
			this.map.put( rowKey, row );
		}
	}

	@Override
	public Tuple get(RowKey column) {
		DBObject row = this.map.get( column );
		DBObject dbTuple = (DBObject)row.get( MongoDBDialect.TUPLE_FIELDNAME );
		return new Tuple( new MongoDBTupleSnapshot( dbTuple ) );
	}

	//not for embedded
	public DBObject getQueryObject() {
		DBObject query = new BasicDBObject();
		query.put( MongoDBDialect.ID_FIELDNAME, assoc.get( MongoDBDialect.ID_FIELDNAME ) );
		return query;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return map.containsKey( column );
	}

	@Override
	public int size() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	public Collection<DBObject> getRows() {
		return (Collection<DBObject>)assoc.get( MongoDBDialect.ROWS_FIELDNAME );
	}

	public DBObject getRowKeyDBObject(RowKey rowKey) {
		return map.get( rowKey );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return map.keySet();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MongoDBAssociationSnapshot(" );
		sb.append( map.size() );
		sb.append( " RowKey entries)." );
		return sb.toString();
	}
}