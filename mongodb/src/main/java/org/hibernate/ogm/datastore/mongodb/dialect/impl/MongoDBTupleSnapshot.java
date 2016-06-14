/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;

import com.mongodb.DBObject;

/**
 * A {@link TupleSnapshot} based on a {@link DBObject} retrieved from MongoDB.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Christopher Auston
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	private final DBObject dbObject;
	private final EntityKeyMetadata keyMetadata;
	private SnapshotType snapshotType;

	public MongoDBTupleSnapshot(DBObject dbObject, EntityKeyMetadata meta, SnapshotType snapshotType) {
		this.dbObject = dbObject;
		this.keyMetadata = meta;
		this.snapshotType = snapshotType;
	}

	public DBObject getDbObject() {
		return dbObject;
	}

	@Override
	public Set<String> getColumnNames() {
		return dbObject.keySet();
	}

	@Override
	public SnapshotType getSnapshotType() {
		return snapshotType;
	}

	public void setSnapshotType(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
	}

	@Override
	public boolean isEmpty() {
		return dbObject.keySet().isEmpty();
	}

	public boolean isKeyColumn(String column) {
		return keyMetadata != null && keyMetadata.isKeyColumn( column );
	}

	@Override
	public Object get(String column) {
		return isKeyColumn( column ) ? getKeyColumnValue( column ) : getValue( dbObject, column );
	}

	private Object getKeyColumnValue(String column) {
		Object idField = dbObject.get( MongoDBDialect.ID_FIELDNAME );

		// single-column key will be stored as is
		if ( keyMetadata.getColumnNames().length == 1 ) {
			return idField;
		}
		// multi-column key nested within DBObject
		else {
			// the name of the column within the id object
			if ( column.contains( MongoDBDialect.PROPERTY_SEPARATOR ) ) {
				column = column.substring( column.indexOf( MongoDBDialect.PROPERTY_SEPARATOR ) + 1 );
			}

			return getValue( (DBObject) idField, column );
		}
	}

	/**
	 * The internal structure of a {@link DBOject} is like a tree. Each embedded object is a new {@code DBObject}
	 * itself. We traverse the tree until we've arrived at a leaf and retrieve the value from it.
	 */
	private Object getValue(DBObject dbObject, String column) {
		Object valueOrNull = MongoHelpers.getValueOrNull( dbObject, column );
		return valueOrNull;
	}
}
