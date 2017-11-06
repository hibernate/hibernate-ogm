/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;

/**
 * Common base class for GeoJSON objects.
 *
 * @author Guillaume Smet
 */
public abstract class AbstractGeoJsonObject implements Serializable {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private String type;

	protected AbstractGeoJsonObject(String type) {
		this.type = type;
	}

	public BsonDocument toBsonDocument() {
		BsonDocument document = new BsonDocument();
		document.put( "type", new BsonString( type ) );
		document.put( "coordinates", toCoordinates() );
		return document;
	}

	protected abstract BsonArray toCoordinates();

	protected static void checkType(String expectedType, Document document) {
		String documentType = document.getString( "type" );
		if ( !expectedType.equals( documentType ) ) {
			throw log.invalidGeoJsonType( documentType, expectedType );
		}
	}
}
