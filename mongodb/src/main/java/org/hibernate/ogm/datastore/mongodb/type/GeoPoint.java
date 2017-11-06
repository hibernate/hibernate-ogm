/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type;

import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonDouble;
import org.bson.Document;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Type used to represent a GeoJSON Point in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoPoint extends AbstractGeoJsonObject {

	private static final String TYPE = "Point";

	/**
	 * The longitude of the point.
	 */
	private double longitude;

	/**
	 * The latitude of the point.
	 */
	private double latitude;

	/**
	 * Instantiates a new Point.
	 *
	 * @param longitude the longitude of the point
	 * @param latitude the latitude of the point
	 */
	public GeoPoint(double longitude, double latitude) {
		super( TYPE );
		Contracts.assertNotNull( longitude, "longitude" );
		Contracts.assertNotNull( latitude, "latitude" );
		this.longitude = longitude;
		this.latitude = latitude;
	}

	/**
	 * @return the longitude of the point
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return the latitude of the point
	 */
	public double getLatitude() {
		return latitude;
	}


	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();

		coordinates.add( new BsonDouble( longitude ) );
		coordinates.add( new BsonDouble( latitude ) );

		return coordinates;
	}

	static GeoPoint fromCoordinates(List<Double> coordinates) {
		if ( coordinates == null ) {
			return null;
		}

		return new GeoPoint( coordinates.get( 0 ), coordinates.get( 1 ) );
	}

	@SuppressWarnings("unchecked")
	public static GeoPoint fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<Double> coordinates = (List<Double>) document.get( "coordinates" );

		return fromCoordinates( coordinates );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		GeoPoint that = (GeoPoint) obj;

		if ( that.longitude != longitude ) {
			return false;
		}
		if ( that.latitude != latitude ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = Double.valueOf( longitude ).hashCode();
		hashCode = hashCode * 31 + Double.valueOf( latitude ).hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		return "GeoPoint [longitude=" + longitude + ", latitude=" + latitude + "]";
	}
}
