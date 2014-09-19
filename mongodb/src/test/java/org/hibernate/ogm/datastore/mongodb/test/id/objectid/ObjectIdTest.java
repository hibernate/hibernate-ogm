/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import static org.fest.assertions.Assertions.assertThat;

import org.bson.types.ObjectId;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for using object ids with MongoDB.
 *
 * @author Gunnar Morling
 *
 */
public class ObjectIdTest extends OgmTestCase {

	@Test
	public void canUseManuallyAssignedObjectId() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		BarKeeper brian = new BarKeeper( new ObjectId(), "Brian" );

		// when
		session.persist( brian );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		BarKeeper brianLoaded = (BarKeeper) session.load( BarKeeper.class, brian.getId() );

		// then
		assertThat( brianLoaded.getId() ).isEqualTo( brian.getId() );
		assertThat( brianLoaded.getName() ).isEqualTo( "Brian" );

		tx.commit();
		session.close();
	}

	@Test
	public void canUseManuallyAssignedObjectIdInAssociation() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		BarKeeper brian = new BarKeeper( new ObjectId(), "Brian" );
		Drink cubaLibre = new Drink( new ObjectId(), "Cuba Libre" );
		brian.setFavoriteDrink( cubaLibre );

		// when
		session.persist( brian );
		session.persist( cubaLibre );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		BarKeeper brianLoaded = (BarKeeper) session.load( BarKeeper.class, brian.getId() );

		// then
		assertThat( brianLoaded.getName() ).isEqualTo( "Brian" );
		assertThat( brianLoaded.getFavoriteDrink() ).isNotNull();
		assertThat( brianLoaded.getFavoriteDrink().getName() ).isEqualTo( "Cuba Libre" );

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BarKeeper.class, Drink.class };
	}
}
