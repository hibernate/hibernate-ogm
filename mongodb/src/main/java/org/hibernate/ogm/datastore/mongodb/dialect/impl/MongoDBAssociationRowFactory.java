/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRowFactory;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRow.AssociationRowAccessor;
import org.hibernate.ogm.datastore.document.association.spi.SingleColumnAwareAssociationRowFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * {@link AssociationRowFactory} which creates association rows based on the {@link DBObject} based representation used
 * in MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBAssociationRowFactory extends SingleColumnAwareAssociationRowFactory<DBObject> {

	public static final MongoDBAssociationRowFactory INSTANCE = new MongoDBAssociationRowFactory();

	private MongoDBAssociationRowFactory() {
		super( DBObject.class );
	}

	@Override
	protected DBObject getSingleColumnRow(String columnName, Object value) {
		DBObject dbObjectAsRow = new BasicDBObject(1);
		MongoHelpers.setValue( dbObjectAsRow, columnName, value );
		return dbObjectAsRow;
	}

	@Override
	protected AssociationRowAccessor<DBObject> getAssociationRowAccessor() {
		return MongoDBAssociationRowAccessor.INSTANCE;
	}

	private static class MongoDBAssociationRowAccessor implements AssociationRow.AssociationRowAccessor<DBObject> {

		private static final MongoDBAssociationRowAccessor INSTANCE = new MongoDBAssociationRowAccessor();

		@Override
		public Set<String> getColumnNames(DBObject row) {
			Set<String> columnNames = new HashSet<String>();
			addColumnNames( row, columnNames, "" );
			return columnNames;
		}

		private void addColumnNames(DBObject row, Set<String> columnNames, String prefix) {
			for ( String field : row.keySet() ) {
				Object sub = row.get( field );
				if ( sub instanceof DBObject ) {
					addColumnNames( (DBObject) sub, columnNames, MongoHelpers.flatten( prefix, field ) );
				}
				else {
					columnNames.add( MongoHelpers.flatten( prefix, field ) );
				}
			}
		}

		@Override
		public Object get(DBObject row, String column) {
			return MongoHelpers.getValueOrNull( row, column );
		}
	}
}
