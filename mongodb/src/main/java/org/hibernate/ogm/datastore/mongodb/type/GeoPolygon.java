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
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Type used to represent a GeoJSON Polygon in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoPolygon extends AbstractGeoJsonObject {

	private static final String TYPE = "Polygon";

	private List<List<GeoPoint>> rings = new ArrayList<>( 1 );

	/**
	 * Instantiates a new Polygon.
	 *
	 * @param exteriorRing the exterior ring of the polygon, it cannot self-intersect
	 */
	public GeoPolygon(List<GeoPoint> exteriorRing) {
		super( TYPE );
		Contracts.assertNotNull( exteriorRing, "exteriorRing" );
		this.rings.add( exteriorRing );
	}

	/**
	 * Instantiates a new Polygon.
	 *
	 * @param firstPoint the first point of the polygon
	 * @param secondPoint the second point of the polygon
	 * @param thirdPoint the third point of the polygon
	 * @param fourthPoint the third point of the polygon
	 * @param additionalPoints the additional points of the polygon
	 */
	public GeoPolygon(GeoPoint firstPoint, GeoPoint secondPoint, GeoPoint thirdPoint, GeoPoint fourthPoint, GeoPoint... additionalPoints) {
		super( TYPE );
		Contracts.assertNotNull( firstPoint, "firstPoint" );
		Contracts.assertNotNull( secondPoint, "secondPoint" );
		Contracts.assertNotNull( thirdPoint, "thirdPoint" );
		Contracts.assertNotNull( fourthPoint, "fourthPoint" );
		Contracts.assertNotNull( additionalPoints, "additionalPoints" );

		List<GeoPoint> exteriorRing = new ArrayList<>( 4 + additionalPoints.length );
		exteriorRing.add( firstPoint );
		exteriorRing.add( secondPoint );
		exteriorRing.add( thirdPoint );
		exteriorRing.add( fourthPoint );
		exteriorRing.addAll( Arrays.asList( additionalPoints ) );

		this.rings.add( exteriorRing );
	}

	/**
	 * Adds a new hole to the polygon.
	 *
	 * @param hole a hole, must be contained in the exterior ring and must not overlap or
	 * intersect another hole
	 * @return this for chaining
	 */
	public GeoPolygon addHole(List<GeoPoint> hole) {
		Contracts.assertNotNull( hole, "hole" );
		this.rings.add( hole );
		return this;
	}

	/**
	 * Adds a new hole to the polygon.
	 *
	 * @param firstPoint the first point of the polygon
	 * @param secondPoint the second point of the polygon
	 * @param thirdPoint the third point of the polygon
	 * @param fourthPoint the third point of the polygon
	 * @param additionalPoints the additional points of the polygon
	 */
	public GeoPolygon addHole(GeoPoint firstPoint, GeoPoint secondPoint, GeoPoint thirdPoint, GeoPoint fourthPoint, GeoPoint... additionalPoints) {
		Contracts.assertNotNull( firstPoint, "firstPoint" );
		Contracts.assertNotNull( secondPoint, "secondPoint" );
		Contracts.assertNotNull( thirdPoint, "thirdPoint" );
		Contracts.assertNotNull( fourthPoint, "fourthPoint" );
		Contracts.assertNotNull( additionalPoints, "additionalPoints" );

		List<GeoPoint> hole = new ArrayList<>( 4 + additionalPoints.length );
		hole.add( firstPoint );
		hole.add( secondPoint );
		hole.add( thirdPoint );
		hole.add( fourthPoint );
		hole.addAll( Arrays.asList( additionalPoints ) );

		this.rings.add( hole );

		return this;
	}

	/**
	 * Adds new holes to the polygon.
	 *
	 * @param holes holes, must be contained in the exterior ring and must not overlap or
	 * intersect another hole
	 * @return this for chaining
	 */
	public GeoPolygon addHoles(List<List<GeoPoint>> holes) {
		Contracts.assertNotNull( holes, "holes" );
		this.rings.addAll( holes );
		return this;
	}

	/**
	 * Returns the external ring of the polygon.
	 *
	 * @return the external ring
	 */
	public List<GeoPoint> getExternalRing() {
		return rings.get( 0 );
	}

	/**
	 * @return the rings of the polygon
	 */
	public List<List<GeoPoint>> getRings() {
		return rings;
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();
		for ( List<GeoPoint> ring : rings ) {
			BsonArray ringCoordinates = new BsonArray();
			for ( GeoPoint geoPoint : ring ) {
				ringCoordinates.add( geoPoint.toCoordinates() );
			}
			coordinates.add( ringCoordinates );
		}
		return coordinates;
	}

	static GeoPolygon fromCoordinates(List<List<List<Double>>> coordinates) {
		if ( coordinates == null ) {
			return null;
		}

		List<List<GeoPoint>> rings = new ArrayList<>( coordinates.size() );

		for ( List<List<Double>> ringCoordinates : coordinates ) {
			List<GeoPoint> ring = ringCoordinates.stream()
					.map( GeoPoint::fromCoordinates )
					.collect( Collectors.toList() );
			rings.add( ring );
		}

		GeoPolygon geoPolygon = new GeoPolygon( rings.get( 0 ) );
		geoPolygon.addHoles( rings.subList( 1, rings.size() ) );

		return geoPolygon;
	}

	@SuppressWarnings("unchecked")
	public static GeoPolygon fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<List<List<Double>>> coordinates = (List<List<List<Double>>>) document.get( "coordinates" );

		if ( coordinates == null ) {
			return null;
		}

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

		GeoPolygon that = (GeoPolygon) obj;

		if ( !that.rings.equals( rings ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return rings.hashCode();
	}

	@Override
	public String toString() {
		return "GeoPolygon [rings=" + rings + "]";
	}
}
