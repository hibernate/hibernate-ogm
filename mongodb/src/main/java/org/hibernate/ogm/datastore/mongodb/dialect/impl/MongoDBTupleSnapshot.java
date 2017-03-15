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

import org.bson.Document;


/**
 * A {@link TupleSnapshot} based on a {@link Document} retrieved from MongoDB.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Christopher Auston
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBTupleSnapshot implements TupleSnapshot {

	public static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	private final Document dbObject;
	private final EntityKeyMetadata keyMetadata;

	public MongoDBTupleSnapshot(Document dbObject, EntityKeyMetadata meta) {
		this.dbObject = dbObject;
		this.keyMetadata = meta;
	}

	public Document getDbObject() {
		return dbObject;
	}

	@Override
	public Set<String> getColumnNames() {
		return dbObject.keySet();
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
		// multi-column key nested within Document
		else {
			// the name of the column within the id object
			if ( column.contains( MongoDBDialect.PROPERTY_SEPARATOR ) ) {
				column = column.substring( column.indexOf( MongoDBDialect.PROPERTY_SEPARATOR ) + 1 );
			}

			return getValue( (Document) idField, column );
		}
	}

	/**
	 * The internal structure of a {@link Document} is like a tree. Each embedded object is a new {@code Document}
	 * itself. We traverse the tree until we've arrived at a leaf and retrieve the value from it.
	 */
	private Object getValue(Document dbObject, String column) {
		Object valueOrNull = MongoHelpers.getValueOrNull( dbObject, column );
		return valueOrNull;
	}
}
