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
 * Type used to represent a GeoJSON MultiLineString in MongoDB and support spatial queries.
 *
 * @author Guillaume Smet
 */
@Experimental
public class GeoMultiLineString extends AbstractGeoJsonObject {

	private static final String TYPE = "MultiLineString";

	/**
	 * The list of LineStrings.
	 */
	private List<GeoLineString> lineStrings;

	/**
	 * Instantiates a new MultiLineString.
	 *
	 * @param lineStrings the list of LineStrings
	 */
	public GeoMultiLineString(List<GeoLineString> lineStrings) {
		super( TYPE );
		this.lineStrings = lineStrings;
	}

	/**
	 * Instantiates a new MultiLineString.
	 *
	 * @param firstLineString the first LineString
	 * @param additionalLineStrings the additional LineStrings
	 */
	public GeoMultiLineString(GeoLineString firstLineString, GeoLineString... additionalLineStrings) {
		super( TYPE );
		Contracts.assertNotNull( firstLineString, "firstLineString" );
		Contracts.assertNotNull( additionalLineStrings, "additionalLineStrings" );
		this.lineStrings = new ArrayList<>( Arrays.asList( ArrayHelper.concat( firstLineString, additionalLineStrings ) ) );
	}

	/**
	 * @return the list of LineStrings
	 */
	public List<GeoLineString> getLineStrings() {
		return lineStrings;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		GeoMultiLineString that = (GeoMultiLineString) obj;

		if ( !that.lineStrings.equals( lineStrings ) ) {
			return false;
		}

		return true;
	}

	@Override
	protected BsonArray toCoordinates() {
		BsonArray coordinates = new BsonArray();

		for ( GeoLineString geoLineString : lineStrings ) {
			coordinates.add( geoLineString.toCoordinates() );
		}

		return coordinates;
	}

	@SuppressWarnings("unchecked")
	public static GeoMultiLineString fromDocument(Document document) {
		if ( document == null ) {
			return null;
		}

		checkType( TYPE, document );

		List<List<List<Double>>> linesCoordinates = (List<List<List<Double>>>) document.get( "coordinates" );

		if ( linesCoordinates == null ) {
			return null;
		}

		List<GeoLineString> geoLineStrings = linesCoordinates.stream()
				.map( GeoLineString::fromCoordinates )
				.collect( Collectors.toList() );

		return new GeoMultiLineString( geoLineStrings );
	}

	@Override
	public int hashCode() {
		return lineStrings.hashCode();
	}

	@Override
	public String toString() {
		return "GeoMultiLineString [lineStrings=" + lineStrings + "]";
	}
}
