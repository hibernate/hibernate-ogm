/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class ListTest extends OgmTestCase {

	Child luke = new Child( "Luke" );
	Child leia = new Child( "Leia" );
	Father father = new Father();
	Race race = new Race();

	GrandMother grandMother = new GrandMother();

	@After
	public void after() {
		delete( race.getRunnersByArrival().toArray() );
		delete( luke, leia, father, grandMother, race );
		checkCleanCache();
	}

	private void delete(Object... entities) {
		for ( Object entity : entities ) {
			if ( entity != null ) {
				inTransaction( session -> {
					session.delete( entity );
				} );
			}
		}
	}

	@Test
	public void testOrderedList() throws Exception {
		father.getOrderedChildren().add( luke );
		father.getOrderedChildren().add( null );
		father.getOrderedChildren().add( leia );

		inTransaction( session -> {
			session.persist( luke );
			session.persist( leia );
			session.persist( father );
		} );

		inTransaction( session -> {
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
		} );
	}

	@Test
	public void testUpdateToElementOfOrderedListIsApplied() throws Exception {
		// insert entity with embedded collection
		inTransaction( session -> {
			grandMother.getGrandChildren().add( new GrandChild( "Luke" ) );
			grandMother.getGrandChildren().add( new GrandChild( "Leia" ) );
			session.persist( grandMother );
		} );

		// do an update to one of the elements
		inTransaction( session -> {
			GrandMother nana = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
			assertThat( nana.getGrandChildren() ).onProperty( "name" ).containsExactly( "Luke", "Leia" );
			nana.getGrandChildren().get( 0 ).setName( "Lisa" );
		} );

		inTransaction( session -> {
			// assert update has been propagated
			GrandMother nana = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
			assertThat( nana.getGrandChildren() ).onProperty( "name" ).containsExactly( "Lisa", "Leia" );
		} );
	}

	@Test
	public void testRemovalOfElementFromOrderedListIsApplied() throws Exception {
		// insert entity with embedded collection
		inTransaction( session -> {
			grandMother.getGrandChildren().add( new GrandChild( "Luke" ) );
			grandMother.getGrandChildren().add( new GrandChild( "Leia" ) );
			session.persist( grandMother );
		} );

		// remove one of the elements
		inTransaction( session -> {
			GrandMother nana = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
			nana.getGrandChildren().remove( 0 );
		} );

		// assert removal has been propagated
		inTransaction( session -> {
			grandMother = (GrandMother) session.get( GrandMother.class, grandMother.getId() );
			assertThat( grandMother.getGrandChildren() ).onProperty( "name" ).containsExactly( "Leia" );
		} );
	}

	@Test
	public void testOrderedListAndCompositeId() throws Exception {

		inTransaction( session -> {
			Runner emmanuel = new Runner( "Emmanuel", "Bernard", 37 );
			Runner pere = new Runner( "Pere", "Noel", 105 );

			race.setRaceId( new Race.RaceId( 23, 75 ) );
			race.getRunnersByArrival().add( emmanuel );
			race.getRunnersByArrival().add( pere );

			session.persist( race );
			session.persist( emmanuel );
			session.persist( pere );
		} );

		inTransaction( session -> {
			Race marathon = (Race) session.get( Race.class, race.getRaceId() );
			assertThat( marathon.getRunnersByArrival() ).hasSize( 2 );
			assertThat( marathon.getRunnersByArrival().get( 0 ).getRunnerId().getFirstname() ).isEqualTo( "Emmanuel" );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Father.class,
				GrandMother.class,
				Child.class,
				Race.class,
				Runner.class
		};
	}
}
