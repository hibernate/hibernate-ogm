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
 * @author Guillaume Smet
 * @author Aleksandr Mylnikov
 */
public abstract class AbstractGeoJsonObject implements Serializable {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
     * Name of the main key used in the JSON document.
     * For most geo objects, it is {@code coordinates}. For {@link GeoCollection}, it is {@code geometries}.
	 */
	private final String geoObjectDataKey;

	private String type;

	protected AbstractGeoJsonObject(String type) {
		this.type = type;
		this.geoObjectDataKey = "coordinates";
	}

	protected AbstractGeoJsonObject(String type, String keyName) {
		this.type = type;
		this.geoObjectDataKey = keyName;
	}

	public BsonDocument toBsonDocument() {
		BsonDocument document = new BsonDocument();
		document.put( "type", new BsonString( type ) );
		document.put( geoObjectDataKey, toCoordinates() );
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
