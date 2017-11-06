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
import org.hibernate.ogm.datastore.mongodb.type.GeoPoint;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Demonstrate the spatial support for MongoDB.
 *
 * @author Guillaume Smet
 */
public class MongoDBSpatialQueryTest extends OgmTestCase {

	private final Restaurant ourson = new Restaurant( 1L, "L'ourson qui boit", new GeoPoint( 4.835195, 45.7706477 ) );
	private final Restaurant margotte = new Restaurant( 2L, "Chez Margotte", new GeoPoint( 4.8510299, 45.7530374 ) );
	private final Restaurant imouto = new Restaurant( 3L, "Imouto", new GeoPoint( 4.8386221, 45.7541719 ) );

	@Before
	public void init() {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.persist( ourson );
			session.persist( margotte );
			session.persist( imouto );
			tx.commit();
		}
	}

	@After
	public void tearDown() throws InterruptedException {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			delete( session, ourson );
			delete( session, margotte );
			delete( session, imouto );
			tx.commit();
		}
	}

	private void delete(final Session session, final Restaurant restaurant) {
		Object entity = session.get( Restaurant.class, restaurant.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSpatialQuery() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			Query query = session
					.createNativeQuery( "{ location: { $near: { $geometry: { type: 'Point', coordinates: [4.8520035, 45.7498209] }, $maxDistance: 500 } } }" )
					.addEntity( Restaurant.class );
			List<Restaurant> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly( 2L );

			query = session
					.createNativeQuery( "{ location: { $near: { $geometry: { type: 'Point', coordinates: [4.8520035, 45.7498209] }, $maxDistance: 2000 } } }" )
					.addEntity( Restaurant.class );
			result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly( 2L, 3L );

			transaction.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Restaurant.class };
	}
}
