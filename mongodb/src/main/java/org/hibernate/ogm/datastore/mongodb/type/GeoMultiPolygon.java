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
 * Type used to represent a GeoJSON MultiPolygon in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoMultiPolygon extends AbstractGeoJsonObject {

	private static final String TYPE = "MultiPolygon";

	/**
	 * The list of Polygon.
	 */
	private List<GeoPolygon> polygons;

	/**
	 * Instantiates a new MultiPolygon.
	 *
	 * @param polygons the list of Polygons
	 */
	public GeoMultiPolygon(List<GeoPolygon> polygons) {
		super( TYPE );
		this.polygons = polygons;
	}

	/**
	 * Instantiates a new MultiPolygon.
	 *
	 * @param firstPolygon the first Polygon
	 * @param additionalPolygons the additional Polygons
	 */
	public GeoMultiPolygon(GeoPolygon firstPolygon, GeoPolygon... additionalPolygons) {
		super( TYPE );
		Contracts.assertNotNull( firstPolygon, "firstPolygon" );
		Contracts.assertNotNull( additionalPolygons, "additionalPolygons" );
		this.polygons = new ArrayList<>( Arrays.asList( ArrayHelper.concat( firstPolygon, additionalPolygons ) ) );
	}

	/**
	 * @return the list of Polygons
	 */
	public List<GeoPolygon> getPolygons() {
		return polygons;
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();

		for ( GeoPolygon geoPolygon : polygons ) {
			coordinates.add( geoPolygon.toCoordinates() );
		}

		return coordinates;
	}

	@SuppressWarnings("unchecked")
	public static GeoMultiPolygon fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<List<List<List<Double>>>> polygonsCoordinates = (List<List<List<List<Double>>>>) document.get( "coordinates" );

		if ( polygonsCoordinates == null ) {
			return null;
		}

		List<GeoPolygon> geoPolygons = polygonsCoordinates.stream()
				.map( GeoPolygon::fromCoordinates )
				.collect( Collectors.toList() );

		return new GeoMultiPolygon( geoPolygons );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		GeoMultiPolygon that = (GeoMultiPolygon) obj;

		if ( !that.polygons.equals( polygons ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return polygons.hashCode();
	}

	@Override
	public String toString() {
		return "GeoMultiPolygon [polygons=" + polygons + "]";
	}
}
