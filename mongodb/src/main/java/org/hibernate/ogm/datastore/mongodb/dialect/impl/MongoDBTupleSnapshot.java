/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.getValueFromColumns;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	/**
	 * Identify the purpose for the creation of a {@link MongoDBTupleSnapshot}
	 *
	 * @author Davide D'Alto <davide@hibernate.org>
	 */
	public enum SnapshotType {
		INSERT, UPDATE, SELECT
	}

	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	private final DBObject dbObject;
	private final RowKey rowKey;
	private final EntityKey entityKey;
	//use it so it avoids multiple calls to Arrays.asList()
	private final List<String> columnNames;
	private final SnapshotType operationType;

	//consider RowKey columns and values as part of the Tuple
	public MongoDBTupleSnapshot(DBObject dbObject, RowKey rowKey, SnapshotType operationType) {
		this.dbObject = dbObject;
		this.rowKey = rowKey;
		this.operationType = operationType;
		this.entityKey = null;
		this.columnNames = null;
	}

	public MongoDBTupleSnapshot(DBObject dbObject, EntityKey entityKey, SnapshotType operationType) {
		this.dbObject = dbObject;
		this.entityKey = entityKey;
		this.operationType = operationType;
		this.columnNames  = Arrays.asList( entityKey.getColumnNames());
		this.rowKey = null;
	}

	@Override
	public Object get(String column) {
		//if the column requested is from the RowKey metadata, get it form there
		if ( rowKey != null && ! isEmpty() ) {
			Object result = getValueFromColumns( column, rowKey.getColumnNames(), rowKey.getColumnValues() );
			if ( result != null ) {
				return result;
			}
		}
		//otherwise get it from the object
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
		//add the columns from the rowKey info as the datastore structure might be incomplete
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

	public SnapshotType getOperationType() {
		return operationType;
	}

}
