/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class ListTest extends OgmTestCase {

	@Test
	public void testOrderedList() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Child luke = new Child();
		luke.setName( "Luke" );
		Child leia = new Child();
		leia.setName( "Leia" );
		session.persist( luke );
		session.persist( leia );
		Father father = new Father();
		father.getOrderedChildren().add( luke );
		father.getOrderedChildren().add( null );
		father.getOrderedChildren().add( leia );
		session.persist( father );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		father = (Father) session.get( Father.class, father.getId() );
		assertThat( father.getOrderedChildren() )
				.as( "List should have 3 elements" )
				.hasSize( 3 );
		assertThat( father.getOrderedChildren().get( 0 ).getName() )
				.as( "Luke should be first" )
				.isEqualTo( luke.getName() );
		assertThat( father.getOrderedChildren().get( 1 ) )
				.as( "Second born should be null" )
				.isNull();
		assertThat( father.getOrderedChildren().get( 2 ).getName() )
				.as( "Leia should be third" )
				.isEqualTo( leia.getName() );
		session.delete( father );
		session.delete( session.load( Child.class, luke.getId() ) );
		session.delete( session.load( Child.class, leia.getId() ) );
		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testUpdateToElementOfOrderedListIsApplied() throws Exception {
		//insert entity with embedded collection
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		GrandChild luke = new GrandChild();
		luke.setName( "Luke" );
		GrandChild leia = new GrandChild();
		leia.setName( "Leia" );
		GrandMother grandMother = new GrandMother();
		grandMother.getGrandChildren().add( luke );
		grandMother.getGrandChildren().add( leia );
		session.persist( grandMother );
		tx.commit();

		session.clear();

		//do an update to one of the elements
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Luke", "Leia" );
		grandMother.getGrandChildren().get( 0 ).setName( "Lisa" );

		tx.commit();
		session.clear();

		//assert update has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Lisa", "Leia" );

		session.delete( grandMother );
		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testRemovalOfElementFromOrderedListIsApplied() throws Exception {
		//insert entity with embedded collection
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		GrandChild luke = new GrandChild();
		luke.setName( "Luke" );
		GrandChild leia = new GrandChild();
		leia.setName( "Leia" );
		GrandMother grandMother = new GrandMother();
		grandMother.getGrandChildren().add( luke );
		grandMother.getGrandChildren().add( leia );
		session.persist( grandMother );
		tx.commit();

		session.clear();

		//remove one of the elements
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		grandMother.getGrandChildren().remove( 0 );
		tx.commit();
		session.clear();

		//assert removal has been propagated
		tx = session.beginTransaction();
		grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
		assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );

		session.delete( grandMother );
		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testOrderedListAndCompositeId() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Race race = new Race();
		race.setRaceId( new Race.RaceId( 23, 75 ) );
		Runner runner = new Runner();
		runner.setAge( 37 );
		runner.setRunnerId( new Runner.RunnerId( "Emmanuel", "Bernard" ) );
		Runner runner2 = new Runner();
		runner2.setAge( 105 );
		runner2.setRunnerId( new Runner.RunnerId( "Pere", "Noel" ) );
		race.getRunnersByArrival().add( runner );
		race.getRunnersByArrival().add( runner2 );
		session.persist( race );
		session.persist( runner );
		session.persist( runner2 );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		race = (Race) session.get( Race.class, race.getRaceId() );
		assertThat( race.getRunnersByArrival() ).hasSize( 2 );
		assertThat( race.getRunnersByArrival().get( 0 ).getRunnerId().getFirstname() ).isEqualTo( "Emmanuel" );
		session.delete( race.getRunnersByArrival().get( 0 ) );
		session.delete( race.getRunnersByArrival().get( 1 ) );
		session.delete( race );
		transaction.commit();

		session.close();
		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Father.class,
				GrandMother.class,
				Child.class,
				Race.class,
				Runner.class
		};
	}
}
