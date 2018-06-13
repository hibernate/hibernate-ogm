/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.BsonArray;
import org.bson.Document;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Type used to represent a GeoJSON GeometryCollection in MongoDB and support spatial queries.
 *
 * @author Aleksandr Mylnikov
 */
public class GeoCollection extends AbstractGeoJsonObject {

	private static final String TYPE = "GeometryCollection";

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private List<AbstractGeoJsonObject> geometries;

	public GeoCollection(List<AbstractGeoJsonObject> geometries) {
		super( TYPE, "geometries" );
		this.geometries = geometries;
	}

	/**
	 * Instantiates a new GeometryCollection.
	 *
	 * @param firstGeoObject       the first {@link AbstractGeoJsonObject}
	 * @param additionalGeoObjects the additional {@link AbstractGeoJsonObject}
	 */
	public GeoCollection(AbstractGeoJsonObject firstGeoObject, AbstractGeoJsonObject... additionalGeoObjects) {
		super( TYPE );
		Contracts.assertNotNull( firstGeoObject, "firstGeoObject" );
		Contracts.assertNotNull( additionalGeoObjects, "additionalGeoObjects" );
		this.geometries = new ArrayList<>( Arrays.asList( ArrayHelper.concat( firstGeoObject, additionalGeoObjects ) ) );
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();
		for ( AbstractGeoJsonObject geometry : geometries ) {
			coordinates.add( geometry.toBsonDocument() );
		}
		return coordinates;
	}

	@SuppressWarnings("unchecked")
	public static GeoCollection fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}
		checkType( TYPE, document );
		List<Document> geometries = (List<Document>) document.get( "geometries" );
		List<AbstractGeoJsonObject> array = new ArrayList<>();
		for ( Document geometry : geometries ) {
			array.add( getGeoJsonObjectFromDocument( geometry ) );
		}
		return new GeoCollection( array );
	}

	private static AbstractGeoJsonObject getGeoJsonObjectFromDocument(Document document) {
		switch ( (String) document.get( "type" ) ) {
			case GeoLineString.TYPE:
				return GeoLineString.fromDocument( document );
			case GeoMultiLineString.TYPE:
				return GeoMultiLineString.fromDocument( document );
			case GeoMultiPoint.TYPE:
				return GeoMultiPoint.fromDocument( document );
			case GeoMultiPolygon.TYPE:
				return GeoMultiPolygon.fromDocument( document );
			case GeoPoint.TYPE:
				return GeoPoint.fromDocument( document );
			case GeoPolygon.TYPE:
				return GeoPolygon.fromDocument( document );
		}
		throw log.invalidGeoJsonType( (String) document.get( "type" ),
				String.join( ";", GeoLineString.TYPE, GeoMultiLineString.TYPE, GeoMultiPoint.TYPE,
						GeoMultiPolygon.TYPE, GeoPoint.TYPE, GeoPolygon.TYPE ) );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		GeoCollection that = (GeoCollection) obj;

		return that.geometries.equals( geometries );
	}

	@Override
	public int hashCode() {
		return geometries.hashCode();
	}

	@Override
	public String toString() {
		return "GeoCollection [geometries=" + geometries + "]";
	}

}
