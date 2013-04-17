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

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.grid.AssociationKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.hibernate.ogm.grid.AssociationKind;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoHelpers {

	public static DBObject associationKeyToObject(AssociationStorage storage, AssociationKey key) {
		if ( isEmbeddedInEntity( key, storage ) ) {
			throw new AssertionFailure( MongoHelpers.class.getName()
					+ ".associationKeyToObject should not be called for associations embedded in entity documents");
		}
		Object[] columnValues = key.getColumnValues();
		DBObject columns = new BasicDBObject( columnValues.length );

		int i = 0;
		for ( String name : key.getColumnNames() ) {
			columns.put( name, columnValues[i++] );
		}

		BasicDBObject idObject = new BasicDBObject( 1 );

		if ( storage == AssociationStorage.GLOBAL_COLLECTION ) {
			columns.put( MongoDBDialect.TABLE_FIELDNAME, key.getTable() );
		}
		idObject.put( "_id", columns );
		return idObject;
	}

	public static boolean isEmbeddedInEntity(AssociationKey key, AssociationStorage storage) {
		return ( key != null && key.getAssociationKind() == AssociationKind.EMBEDDED )
				|| storage == AssociationStorage.IN_ENTITY;
	}

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
