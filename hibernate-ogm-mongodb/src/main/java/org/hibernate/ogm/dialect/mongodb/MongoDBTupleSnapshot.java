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

import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;

import com.mongodb.DBObject;

import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

import static org.hibernate.ogm.dialect.mongodb.MongoHelpers.getValueFromColumns;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	private final DBObject dbObject;
	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );
	private final RowKey rowKey;
	private final EntityKey entityKey;

	//use it so it avoids multiple calls to Arrays.asList()
	private final List<String> columnNames;


	//consider RowKey columns and values as aprt of the Tuple
	public MongoDBTupleSnapshot(DBObject dbObject, RowKey rowKey) {
		this.dbObject = dbObject;
		this.rowKey = rowKey;
		this.entityKey = null;
		this.columnNames = null;
	}

	public MongoDBTupleSnapshot(DBObject dbObject, EntityKey entityKey) {
		this.dbObject = dbObject;
		this.entityKey = entityKey;
		this.columnNames  = Arrays.asList( entityKey.getColumnNames());
		this.rowKey = null;
	}

	@Override
	public Object get(String column) {
		if ( rowKey != null && ! isEmpty() ) {
			Object result = getValueFromColumns( column, rowKey.getColumnNames(), rowKey.getColumnValues() );
			if ( result != null ) {
				return result;
			}
		}
		if ( column.contains( "." ) ) {
			String[] fields = EMBEDDED_FIELDNAME_SEPARATOR.split( column, 0 );
			return this.getObject( this.dbObject.toMap(), fields, 0 );
		}
		else {
			return this.dbObject.get( column );
		}
	}



	@Override
	public Set<String> getColumnNames() {
		Set<String> columns = this.dbObject.toMap().keySet();
		if ( rowKey != null && ! isEmpty() ) {
			columns = new HashSet<String>(columns);
			for ( String column : rowKey.getColumnNames() ) {
				columns.add( column );
			}
		}
		return columns;
	}

	public DBObject getDbObject() {
		return dbObject;
	}

	/**
	 * The internal structure of a DBOject is like a tree.
	 * Each embedded object is a new branch represented by a Map.
	 * This method browses recursively all nodes and returns the leaf value
	 */
	private Object getObject(Map<?, ?> fields, String[] remainingFields, int startIndex) {
		if ( startIndex == remainingFields.length - 1 ) {
			return fields.get( remainingFields[startIndex] );
		}
		else {
			Map<?, ?> subMap = (Map<?, ?>) fields.get( remainingFields[startIndex] );
			if ( subMap != null ) {
				return this.getObject( subMap, remainingFields, ++startIndex );
			}
			else {
				return null;
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return this.dbObject.keySet().isEmpty();
	}

	public boolean columnInIdField(String column) {
		return (this.columnNames == null) ? false : this.columnNames.contains( column );
	}
}
