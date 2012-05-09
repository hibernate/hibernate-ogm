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

import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.grid.AssociationKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 */
public class MongoHelpers {

	public static DBObject associationKeyToObject(AssociationStorage storage,
			AssociationKey key) {
		Object[] columnValues = key.getColumnValues();
		DBObject columns = new BasicDBObject( columnValues.length );

		int i = 0;
		for ( String name : key.getColumnNames() ) {
			columns.put( name, columnValues[i++] );
		}

		DBObject obj = new BasicDBObject( 1 );
		obj.put( MongoDBDialect.COLUMNS_FIELDNAME, columns );
		
		if ( storage == AssociationStorage.GLOBAL_COLLECTION ) {
			obj.put( MongoDBDialect.TABLE_FIELDNAME, key.getTable() );
		}

		return obj;
	}

}
