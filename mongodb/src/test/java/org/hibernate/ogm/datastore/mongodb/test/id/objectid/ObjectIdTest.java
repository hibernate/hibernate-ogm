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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.query.NoSQLQuery;
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

	@Test
	public void canUseObjectIdAssignedUponInsert() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Bar goldFishBar = new Bar( "Goldfisch Bar" );

		// when
		session.persist( goldFishBar );

		tx.commit();
		assertThat( goldFishBar.getId() ).isNotNull();
		session.clear();
		tx = session.beginTransaction();

		Bar barLoaded = (Bar) session.load( Bar.class, goldFishBar.getId() );

		// then
		assertThat( barLoaded.getName() ).isEqualTo( "Goldfisch Bar" );

		tx.commit();
		session.close();
	}

	@Test
	public void canUseObjectIdAssignedUponInsertInAssociation() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		MusicGenre classicRock = new MusicGenre( "Classic Rock" );

		Bar goldFishBar = new Bar( "Goldfisch Bar" );
		goldFishBar.setMusicGenre( classicRock );
		classicRock.getPlayedIn().add( goldFishBar );

		Bar sharkStation = new Bar( "Shark Station" );
		sharkStation.setMusicGenre( classicRock );
		classicRock.getPlayedIn().add( sharkStation );

		// when
		session.persist( classicRock );
		session.persist( goldFishBar );
		session.persist( sharkStation );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		// then
		Bar barLoaded = (Bar) session.load( Bar.class, goldFishBar.getId() );

		assertThat( barLoaded.getName() ).isEqualTo( "Goldfisch Bar" );
		assertThat( barLoaded.getMusicGenre() ).isNotNull();
		assertThat( barLoaded.getMusicGenre().getName() ).isEqualTo( "Classic Rock" );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		MusicGenre genreLoaded = (MusicGenre) session.load( MusicGenre.class, goldFishBar.getMusicGenre().getId() );
		assertThat( genreLoaded.getPlayedIn() ).onProperty( "name" ).containsOnly( "Goldfisch Bar", "Shark Station" );

		tx.commit();
		session.close();
	}

	@Test
	public void canUseObjectIdAssignedUponInsertInOneToManyAssociation() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Bar goldFishBar = new Bar( "Goldfisch Bar" );
		goldFishBar.getDoorMen().add( new DoorMan( "Bruce" ) );
		goldFishBar.getDoorMen().add( new DoorMan( "Dwain" ) );

		// when
		session.persist( goldFishBar );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		// then
		Bar barLoaded = (Bar) session.load( Bar.class, goldFishBar.getId() );
		assertThat( barLoaded.getDoorMen() ).onProperty( "name" ).containsOnly( "Bruce", "Dwain" );

		tx.commit();
		session.close();
	}

	@Test
	public void canUseObjectIdAssignedUponInsertInManyToManyAssociation() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Snack nachos = new Snack( "nachos" );
		Snack frozenYogurt = new Snack( "frozen yogurt" );
		Ingredient milk = new Ingredient( "milk" );
		Ingredient salt = new Ingredient( "salt" );

		nachos.getIngredients().add( salt );
		salt.getContainedIn().add( nachos );

		frozenYogurt.getIngredients().add( milk );
		milk.getContainedIn().add( frozenYogurt );

		frozenYogurt.getIngredients().add( salt );
		salt.getContainedIn().add( frozenYogurt );

		// when
		session.persist( nachos );
		session.persist( frozenYogurt );
		session.persist( milk );
		session.persist( salt );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		// then
		Snack frozenYogurtLoaded = (Snack) session.load( Snack.class, frozenYogurt.getId() );

		assertThat( frozenYogurtLoaded.getName() ).isEqualTo( "frozen yogurt" );
		assertThat( frozenYogurtLoaded.getIngredients() ).onProperty( "name" ).containsOnly( "salt", "milk" );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		Ingredient milkLoaded = (Ingredient) session.load( Ingredient.class, milk.getId() );
		assertThat( milkLoaded.getName() ).isEqualTo( "milk" );
		assertThat( milkLoaded.getContainedIn() ).onProperty( "name" ).containsOnly( "frozen yogurt" );

		Ingredient saltLoaded = (Ingredient) session.load( Ingredient.class, salt.getId() );
		assertThat( saltLoaded.getName() ).isEqualTo( "salt" );
		assertThat( saltLoaded.getContainedIn() ).onProperty( "name" ).containsOnly( "nachos", "frozen yogurt" );

		tx.commit();
		session.close();
	}

	@Test
	public void canUseGenerationTypeAutoWithObjectId() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Singer gloria = new Singer( "Gloria" );

		// when
		session.persist( gloria );

		tx.commit();
		assertThat( gloria.getId() ).isNotNull();
		session.clear();
		tx = session.beginTransaction();

		Singer singerLoaded = (Singer) session.load( Singer.class, gloria.getId() );

		// then
		assertThat( singerLoaded.getName() ).isEqualTo( "Gloria" );

		tx.commit();
		session.close();
	}

	@Test
	public void stringUsedAsIdIsMappedToObjectId() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		Comedian monty = new Comedian( "Monty" );

		// when
		session.persist( monty );

		tx.commit();
		assertThat( monty.getId() ).isNotNull();
		session.clear();
		tx = session.beginTransaction();

		// then
		assertCountQueryResult( session, "db.Comedian.count({ \"_id\" : { \"$oid\" : \"" + monty.getId() + "\" }, \"name\" : \"Monty\" })", 1L);

		Comedian montyLoaded = (Comedian) session.load( Comedian.class, monty.getId() );
		assertThat( ObjectId.isValid( montyLoaded.getId() ) ).isTrue();
		assertThat( montyLoaded.getName() ).isEqualTo( "Monty" );

		tx.commit();
		session.close();
	}

	private void assertCountQueryResult(OgmSession session, String queryString, long expectedCount) {
		NoSQLQuery query = session.createNativeQuery( queryString );
		query.addScalar( "n" );
		long actualCount = (Long) query.list().iterator().next();
		assertThat( actualCount ).describedAs( "Count query didn't yield expected result" ).isEqualTo( expectedCount );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BarKeeper.class, Drink.class, Bar.class, MusicGenre.class, DoorMan.class, Snack.class, Ingredient.class, Singer.class, Comedian.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
	}
}
