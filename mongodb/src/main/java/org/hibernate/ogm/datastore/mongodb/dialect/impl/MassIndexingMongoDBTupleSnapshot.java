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
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKeyMetadata;

import com.mongodb.DBObject;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MassIndexingMongoDBTupleSnapshot implements TupleSnapshot {

	private final DBObject dbObject;
	private final EntityKeyMetadata entityKeyMetadata;

	public MassIndexingMongoDBTupleSnapshot(DBObject dbObject, EntityKeyMetadata entityKeyMetadata) {
		this.dbObject = dbObject;
		this.entityKeyMetadata = entityKeyMetadata;
	}

	@Override
	public Object get(String column) {
		if ( columnInIdField( column ) ) {
			if ( column.contains( MongoDBDialect.PROPERTY_SEPARATOR ) ) {
				int dotIndex = column.indexOf( MongoDBDialect.PROPERTY_SEPARATOR );
				String shortColumnName = column.substring( dotIndex + 1 );
				DBObject idObject = (DBObject) dbObject.get( MongoDBDialect.ID_FIELDNAME );
				return idObject.get( shortColumnName );
			}
			else {
				return dbObject.get( MongoDBDialect.ID_FIELDNAME );
			}
		}
		else {
			return dbObject.get( column );
		}
	}

	@Override
	public boolean isEmpty() {
		return dbObject.keySet().isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return dbObject.keySet();
	}

	public boolean columnInIdField(String column) {
		if ( entityKeyMetadata == null ) {
			return false;
		}

		for ( String idColumn : entityKeyMetadata.getColumnNames() ) {
			if ( idColumn.equals( column ) ) {
				return true;
			}
		}
		return false;
	}
}
