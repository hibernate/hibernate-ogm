/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.getValueOrNull;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRows;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * An association snapshot based on a {@link DBObject} retrieved from MongoDB.
 *
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class MongoDBAssociationSnapshot extends AssociationRows {

	private final DBObject dbObject;

	public MongoDBAssociationSnapshot(DBObject document, AssociationKey associationKey, AssociationStorageStrategy storageStrategy) {
		super( associationKey, getRows( document, associationKey, storageStrategy ), MongoDBAssociationRowFactory.INSTANCE );
		this.dbObject = document;
	}

	//not for embedded
	public DBObject getQueryObject() {
		DBObject query = new BasicDBObject();
		query.put( MongoDBDialect.ID_FIELDNAME, dbObject.get( MongoDBDialect.ID_FIELDNAME ) );
		return query;
	}

	private static Collection<?> getRows(DBObject document, AssociationKey associationKey, AssociationStorageStrategy storageStrategy) {
		Collection<?> rows;

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			if ( associationKey.getMetadata().isOneToOne() ) {
				Object oneToOneValue = getValueOrNull( document, associationKey.getMetadata().getCollectionRole(), Object.class );
				rows = oneToOneValue != null ? Collections.singletonList( oneToOneValue ) : Collections.emptyList();
			}
			else {
				rows = getValueOrNull( document, associationKey.getMetadata().getCollectionRole(), Collection.class );
			}
		}
		else {
			rows = (Collection<?>) document.get( MongoDBDialect.ROWS_FIELDNAME );
		}

		return rows != null ? rows : Collections.emptyList();
	}

	// TODO This only is used for tests; Can we get rid of it?
	public DBObject getDBObject() {
		return this.dbObject;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MongoDBAssociationSnapshot(" );
		sb.append( size() );
		sb.append( ") RowKey entries)." );
		return sb.toString();
	}
}
