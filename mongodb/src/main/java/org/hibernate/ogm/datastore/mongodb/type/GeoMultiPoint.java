/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.BsonArray;
import org.bson.Document;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Type used to represent a GeoJSON MultiPoint in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoMultiPoint extends AbstractGeoJsonObject {

	private static final String TYPE = "MultiPoint";

	/**
	 * The list of Points.
	 */
	private List<GeoPoint> points;

	/**
	 * Instantiates a new MultiPoint.
	 *
	 * @param point the first point
	 */
	public GeoMultiPoint(GeoPoint point) {
		super( TYPE );
		Contracts.assertNotNull( point, "point" );
		this.points.add( point );
	}

	/**
	 * Instantiates a new MultiPoint.
	 *
	 * @param points the list of points
	 */
	public GeoMultiPoint(List<GeoPoint> points) {
		super( TYPE );
		Contracts.assertNotNull( points, "points" );
		this.points = points;
	}

	/**
	 * Instantiates a new MultiPoint.
	 *
	 * @param firstPoint the first point
	 * @param additionalPoints the additional points
	 */
	public GeoMultiPoint(GeoPoint firstPoint, GeoPoint... additionalPoints) {
		super( TYPE );
		Contracts.assertNotNull( firstPoint, "firstPoint" );
		Contracts.assertNotNull( additionalPoints, "additionalPoints" );
		this.points = new ArrayList<>( Arrays.asList( ArrayHelper.concat( firstPoint, additionalPoints ) ) );
	}

	/**
	 * Adds a new point.
	 *
	 * @param point a point
	 * @return this for chaining
	 */
	public GeoMultiPoint addPoint(GeoPoint point) {
		Contracts.assertNotNull( point, "point" );
		this.points.add( point );
		return this;
	}

	/**
	 * @return the list of points
	 */
	public List<GeoPoint> getPoints() {
		return points;
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();

		for ( GeoPoint geoPoint : points ) {
			coordinates.add( geoPoint.toCoordinates() );
		}

		return coordinates;
	}

	@SuppressWarnings("unchecked")
	public static GeoMultiPoint fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<List<Double>> pointsCoordinates = (List<List<Double>>) document.get( "coordinates" );

		if ( pointsCoordinates == null ) {
			return null;
		}

		List<GeoPoint> geoPoints = pointsCoordinates.stream()
				.map( GeoPoint::fromCoordinates )
				.collect( Collectors.toList() );

		return new GeoMultiPoint( geoPoints );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		GeoMultiPoint that = (GeoMultiPoint) obj;

		if ( !that.points.equals( points ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return points.hashCode();
	}

	@Override
	public String toString() {
		return "GeoMultiPoint [points=" + points + "]";
	}
}
