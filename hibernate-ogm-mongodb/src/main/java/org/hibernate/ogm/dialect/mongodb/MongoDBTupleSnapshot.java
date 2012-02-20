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

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;

import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	private DBObject dbObject;
	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	public MongoDBTupleSnapshot(DBObject dbObject) {
		super();
		this.dbObject = dbObject;
	}

	@Override
	public Object get(String column) {
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
		return this.dbObject.toMap().keySet();
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

}
