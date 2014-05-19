/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		for ( String idColumn : entityKeyMetadata.getColumnNames() ) {
			if ( idColumn.equals( column ) ) {
				return true;
			}
		}
		return false;
	}
}
