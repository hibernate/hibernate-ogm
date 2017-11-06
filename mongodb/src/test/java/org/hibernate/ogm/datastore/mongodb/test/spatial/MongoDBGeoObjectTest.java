/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.spatial;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.type.GeoLineString;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiLineString;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiPoint;
import org.hibernate.ogm.datastore.mongodb.type.GeoMultiPolygon;
import org.hibernate.ogm.datastore.mongodb.type.GeoPoint;
import org.hibernate.ogm.datastore.mongodb.type.GeoPolygon;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the GeoJSON objects support for MongoDB.
 *
 * @author Guillaume Smet
 */
public class MongoDBGeoObjectTest extends OgmTestCase {

	private static final GeoPolygon BOUNDING_BOX = new GeoPolygon(
			new GeoPoint( 4.814922, 45.7753612 ),
			new GeoPoint( 4.8160825, 45.7327172 ),
			new GeoPoint( 4.9281299, 45.7211302 ),
			new GeoPoint( 4.8706127, 45.786724 ),
			new GeoPoint( 4.814922, 45.7753612 )
	);

	private static final GeoPoint OURSON_QUI_BOIT = new GeoPoint( 4.835195, 45.7706477 );
	private static final GeoPoint CHEZ_MARGOTTE = new GeoPoint( 4.8510299, 45.7530374 );
	private static final GeoPoint IMOUTO = new GeoPoint( 4.8386221, 45.7541719 );
	private static final GeoPoint PENJAB = new GeoPoint( 4.826229, 45.7611617 );
	private static final GeoPoint ARSENIC = new GeoPoint( 4.8424062, 45.7591419 );

	private static final GeoMultiPoint MULTI_POINT = new GeoMultiPoint( OURSON_QUI_BOIT, CHEZ_MARGOTTE, ARSENIC );

	private static final GeoLineString LINE_STRING = new GeoLineString( OURSON_QUI_BOIT, CHEZ_MARGOTTE );
	private static final GeoMultiLineString MULTI_LINE_STRING = new GeoMultiLineString(
			LINE_STRING,
			new GeoLineString( IMOUTO, PENJAB )
	);

	private static final GeoPolygon POLYGON = new GeoPolygon( OURSON_QUI_BOIT, PENJAB, IMOUTO, OURSON_QUI_BOIT );
	private static final GeoMultiPolygon MULTI_POLYGON = new GeoMultiPolygon(
			POLYGON,
			new GeoPolygon( ARSENIC, CHEZ_MARGOTTE, IMOUTO, ARSENIC )
	);

	private final GeoObject geoObject = new GeoObject( 1L, OURSON_QUI_BOIT, MULTI_POINT, LINE_STRING, MULTI_LINE_STRING, POLYGON, MULTI_POLYGON );

	@Before
	public void init() {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.persist( geoObject );
			tx.commit();
		}
	}

	@After
	public void tearDown() throws InterruptedException {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			delete( session, geoObject );
			tx.commit();
		}
	}

	private void delete(final Session session, final GeoObject geoObject) {
		Object entity = session.get( GeoObject.class, geoObject.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testBoundingBox() throws Exception {
		try ( OgmSession session = openSession() ) {
			checkBoundingBoxResultForField( session, "point" );
			checkBoundingBoxResultForField( session, "multiPoint" );
			checkBoundingBoxResultForField( session, "lineString" );
			checkBoundingBoxResultForField( session, "multiLineString" );
			checkBoundingBoxResultForField( session, "polygon" );
			checkBoundingBoxResultForField( session, "multiPolygon" );
		}
	}

	@SuppressWarnings("unchecked")
	private void checkBoundingBoxResultForField(OgmSession session, String field) {
		Transaction transaction = session.beginTransaction();

		Query query = session
				.createNativeQuery( "{ " + field + ": { $geoWithin: { $geometry: " + BOUNDING_BOX.toBsonDocument() + " } } }" )
				.addEntity( GeoObject.class );
		List<GeoObject> result = query.list();

		assertThat( result ).hasSize( 1 );

		GeoObject geoObject = result.get( 0 );
		assertThat( geoObject.getId() ).describedAs( "id" ).isEqualTo( 1L );
		assertThat( geoObject.getPoint() ).describedAs( "point" ).isEqualTo( OURSON_QUI_BOIT );
		assertThat( geoObject.getMultiPoint() ).describedAs( "multiPoint" ).isEqualTo( MULTI_POINT );
		assertThat( geoObject.getLineString() ).describedAs( "lineString" ).isEqualTo( LINE_STRING );
		assertThat( geoObject.getMultiLineString() ).describedAs( "multiLineString" ).isEqualTo( MULTI_LINE_STRING );
		assertThat( geoObject.getPolygon() ).describedAs( "polygon" ).isEqualTo( POLYGON );
		assertThat( geoObject.getMultiPolygon() ).describedAs( "multiPolygon" ).isEqualTo( MULTI_POLYGON );

		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ GeoObject.class };
	}
}
