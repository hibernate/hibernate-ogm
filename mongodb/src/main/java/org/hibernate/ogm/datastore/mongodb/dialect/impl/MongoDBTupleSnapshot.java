/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import com.mongodb.DBObject;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKeyMetadata;

/**
 *
 * @author caust_000
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	protected final DBObject dbObject;

	private final String[] columnNames;

	public MongoDBTupleSnapshot(DBObject dbObject, String[] columnNames) {
		this.dbObject = dbObject;
		this.columnNames = columnNames;
	}

	public MongoDBTupleSnapshot(DBObject dbObject, EntityKeyMetadata meta) {
		this( dbObject, null == meta ? null : meta.getColumnNames() );
	}

	@Override
	public Set<String> getColumnNames() {
		return dbObject.keySet();
	}

	@Override
	public boolean isEmpty() {
		return dbObject.keySet().isEmpty();
	}

	public boolean columnInIdField(String column) {
		if (columnNames == null) {
			return false;
		}

		for (String idColumn : columnNames) {
			if (idColumn.equals( column )) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The internal structure of a DBOject is like a tree. Each embedded object
	 * is a new branch represented by a Map. This method browses recursively all
	 * nodes and returns the leaf value
	 */
	private Object getObject(Map<?, ?> fields, String[] remainingFields, int startIndex) {
		if (startIndex == remainingFields.length - 1) {
			return fields.get( remainingFields[startIndex] );
		}
		else {
			Map<?, ?> subMap = (Map<?, ?>) fields.get( remainingFields[startIndex] );
			if (subMap != null) {
				return this.getObject( subMap, remainingFields, ++startIndex );
			}
			else {
				return null;
			}
		}
	}

	@Override
	public Object get(String column) {
		if (columnInIdField( column )) {

			if (column.contains( MongoDBDialect.PROPERTY_SEPARATOR )) {
				DBObject idObject = (DBObject) dbObject.get( MongoDBDialect.ID_FIELDNAME );
				int firstSep = column.indexOf( MongoDBDialect.PROPERTY_SEPARATOR );
				String[] idFields = EMBEDDED_FIELDNAME_SEPARATOR.split( column.substring( firstSep + 1 ), 0 );
				return getObject( idObject.toMap(), idFields, 0 );
			}
			else {
				return dbObject.get( MongoDBDialect.ID_FIELDNAME );
			}
		}

		if (column.contains( MongoDBDialect.PROPERTY_SEPARATOR )) {
			String[] fields = EMBEDDED_FIELDNAME_SEPARATOR.split( column, 0 );
			return this.getObject( this.dbObject.toMap(), fields, 0 );
		}
		else {
			return this.dbObject.get( column );
		}
	}
}
