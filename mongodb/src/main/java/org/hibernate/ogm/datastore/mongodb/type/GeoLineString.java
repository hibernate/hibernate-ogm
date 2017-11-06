/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type;

import java.util.Arrays;
import java.util.List;

import org.bson.BsonArray;
import org.bson.Document;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Type used to represent a GeoJSON LineString in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoLineString extends AbstractGeoJsonObject {

	private static final String TYPE = "LineString";

	/**
	 * The start point of the line.
	 */
	private GeoPoint startPoint;

	/**
	 * The end point of the line.
	 */
	private GeoPoint endPoint;

	/**
	 * Instantiates a new LineString.
	 *
	 * @param startPoint the start point of the line
	 * @param endPoint the end point of the line
	 */
	public GeoLineString(GeoPoint startPoint, GeoPoint endPoint) {
		super( TYPE );
		Contracts.assertNotNull( startPoint, "startPoint" );
		Contracts.assertNotNull( endPoint, "endPoint" );
		this.startPoint = startPoint;
		this.endPoint = endPoint;
	}

	/**
	 * @return the start point of the line
	 */
	public GeoPoint getStartPoint() {
		return startPoint;
	}

	/**
	 * @return the start point of the line
	 */
	public GeoPoint getEndPoint() {
		return endPoint;
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray( Arrays.asList(
				startPoint.toCoordinates(),
				endPoint.toCoordinates()
		) );

		return coordinates;
	}

	static GeoLineString fromCoordinates(List<List<Double>> coordinates) {
		if ( coordinates == null ) {
			return null;
		}

		return new GeoLineString( GeoPoint.fromCoordinates( coordinates.get( 0 ) ), GeoPoint.fromCoordinates( coordinates.get( 1 ) ) );
	}

	@SuppressWarnings("unchecked")
	public static GeoLineString fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<List<Double>> coordinates = (List<List<Double>>) document.get( "coordinates" );

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

		GeoLineString that = (GeoLineString) obj;

		if ( !that.startPoint.equals( startPoint ) ) {
			return false;
		}
		if ( !that.endPoint.equals( endPoint ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = startPoint.hashCode();
		hashCode = hashCode * 31 + endPoint.hashCode();
		return hashCode;
	}

	@Override
	public String toString() {
		return "GeoLineString [startPoint=" + startPoint + ", endPoint=" + endPoint + "]";
	}
}
