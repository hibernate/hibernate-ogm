/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;
import static org.hibernate.ogm.utils.TestHelper.get;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.EnumSet;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneTest extends OgmTestCase {

	@Test
	public void testUnidirectionalManyToOne() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		JUG jug = new JUG( "summer_camp" );
		jug.setName( "JUG Summer Camp" );
		session.persist( jug );
		Member emmanuel = new Member( "emmanuel" );
		emmanuel.setName( "Emmanuel Bernard" );
		emmanuel.setMemberOf( jug );
		Member jerome = new Member( "jerome" );
		jerome.setName( "Jerome" );
		jerome.setMemberOf( jug );
		session.persist( emmanuel );
		session.persist( jerome );
		session.flush();
		assertThat( getNumberOfEntities( session ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( session ) ).isEqualTo( expectedAssociations() );
		transaction.commit();
		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( expectedAssociations() );

		session.clear();

		transaction = session.beginTransaction();
		emmanuel = session.get( Member.class, emmanuel.getId() );
		jug = emmanuel.getMemberOf();
		session.delete( emmanuel );
		jerome = session.get( Member.class, jerome.getId() );
		session.delete( jerome );
		session.delete( jug );
		transaction.commit();
		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 0 );

		session.close();

		checkCleanCache();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-931")
	public void canPersistAssociationToDetachedEntity() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// Persist employer
		Employer employer = new Employer();
		employer.setId( "employer-1" );
		employer.setName( "Hibernate" );
		session.save( employer );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// Create employee and associate it with detached employer
		Employee employee = new Employee();
		employee.setId( "employee-1" );
		employee.setName( "DNadar" );
		employee.setEmployer( employer );
		session.save( employee );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// Load association from main side
		Employee loadedEmployee = session.get( Employee.class, "employee-1" );
		assertThat( loadedEmployee.getEmployer().getName() ).isEqualTo( "Hibernate" );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// Load association from inverse side
		Employer loadedEmployer = session.get( Employer.class, "employer-1" );
		assertThat( loadedEmployer.getEmployees() ).onProperty( "name" ).containsOnly( "DNadar" );

		// Clean up
		session.delete( loadedEmployer.getEmployees().iterator().next() );
		session.delete( loadedEmployer );

		transaction.commit();
		session.close();
	}

	private Long expectedAssociations() {
		if ( EnumSet.of( NEO4J_EMBEDDED, NEO4J_REMOTE ).contains( TestHelper.getCurrentDialectType() ) ) {
			// *-to-one associations are mapped with a relationship in Neo4j that will result in the count
			return 2L;
		}
		return 0L;
	}

	@Test
	public void testBidirectionalManyToOneRegular() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		SalesForce force = new SalesForce( "sales_force" );
		force.setCorporation( "Red Hat" );
		session.save( force );
		SalesGuy eric = new SalesGuy( "eric" );
		eric.setName( "Eric" );
		eric.setSalesForce( force );
		force.getSalesGuys().add( eric );
		session.save( eric );
		SalesGuy simon = new SalesGuy( "simon" );
		simon.setName( "Simon" );
		simon.setSalesForce( force );
		force.getSalesGuys().add( simon );
		session.save( simon );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = session.get( SalesForce.class, force.getId() );
		assertNotNull( force.getSalesGuys() );
		assertEquals( 2, force.getSalesGuys().size() );
		simon = session.get( SalesGuy.class, simon.getId() );
		// purposely faulty
		// force.getSalesGuys().remove( simon );
		session.delete( simon );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = session.get( SalesForce.class, force.getId() );
		assertNotNull( force.getSalesGuys() );
		assertEquals( 1, force.getSalesGuys().size() );
		session.delete( force.getSalesGuys().iterator().next() );
		session.delete( force );
		transaction.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testBidirectionalManyToOneRemoval() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		SalesForce force = new SalesForce( "red_hat" );
		force.setCorporation( "Red Hat" );
		session.save( force );
		SalesGuy eric = new SalesGuy( "eric" );
		eric.setName( "Eric" );
		eric.setSalesForce( force );
		force.getSalesGuys().add( eric );
		session.save( eric );
		SalesGuy simon = new SalesGuy( "simon" );
		simon.setName( "Simon" );
		simon.setSalesForce( force );
		force.getSalesGuys().add( simon );
		session.save( simon );
		transaction.commit();
		session.clear();

		// removing one sales guy, leaving the other in place
		transaction = session.beginTransaction();
		force = session.get( SalesForce.class, force.getId() );
		assertEquals( 2, force.getSalesGuys().size() );
		SalesGuy salesGuy = session.get( SalesGuy.class, eric.getId() );
		salesGuy.setSalesForce( null );
		force.getSalesGuys().remove( salesGuy );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = session.get( SalesForce.class, force.getId() );
		assertEquals( 1, force.getSalesGuys().size() );
		salesGuy = force.getSalesGuys().iterator().next();
		assertThat( salesGuy.getName() ).isEqualTo( "Simon" );

		session.delete( session.get( SalesGuy.class, eric.getId() ) );
		session.delete( session.get( SalesGuy.class, simon.getId() ) );
		session.delete( force );
		transaction.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testBiDirManyToOneInsertUpdateFalse() throws Exception {
		final Session session = openSession();
		Transaction tx = session.beginTransaction();
		Beer hoegaarden = new Beer();
		Brewery hoeBrewery = new Brewery();
		hoeBrewery.getBeers().add( hoegaarden );
		hoegaarden.setBrewery( hoeBrewery );
		session.persist( hoeBrewery );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		hoegaarden = get( session, Beer.class, hoegaarden.getId() );
		assertThat( hoegaarden ).isNotNull();
		assertThat( hoegaarden.getBrewery() ).isNotNull();
		assertThat( hoegaarden.getBrewery().getBeers() )
			.hasSize( 1 )
			.containsOnly( hoegaarden );
		Beer citron = new Beer();
		hoeBrewery = hoegaarden.getBrewery();
		hoeBrewery.getBeers().remove( hoegaarden );
		hoeBrewery.getBeers().add( citron );
		citron.setBrewery( hoeBrewery );
		session.delete( hoegaarden );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		citron = get( session, Beer.class, citron.getId() );
		assertThat( citron.getBrewery().getBeers() )
			.hasSize( 1 )
			.containsOnly( citron );
		hoeBrewery = citron.getBrewery();
		citron.setBrewery( null );
		hoeBrewery.getBeers().clear();
		session.delete( citron );
		session.delete( hoeBrewery );
		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testRemovalOfTransientEntityWithAssociation() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();

		SalesForce force = new SalesForce( "red_hat" );
		force.setCorporation( "Red Hat" );
		session.save( force );

		SalesGuy eric = new SalesGuy( "eric" );
		eric.setName( "Eric" );
		eric.setSalesForce( force );
		force.getSalesGuys().add( eric );
		session.save( eric );

		SalesGuy simon = new SalesGuy( "simon" );
		simon.setName( "Simon" );
		simon.setSalesForce( force );
		force.getSalesGuys().add( simon );
		session.save( simon );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		// The classic API allows to delete transient instances;
		// Intentionally not deleting the referencing sales guys
		session.delete( force );
		transaction.commit();

		transaction = session.beginTransaction();

		SalesGuy salesGuy = session.get( SalesGuy.class, "eric" );
		assertThat( salesGuy.getSalesForce() ).describedAs( "Stale association should be exposed as null" ).isNull();
		session.delete( salesGuy );

		salesGuy = session.get( SalesGuy.class, "simon" );
		assertThat( salesGuy.getSalesForce() ).describedAs( "Stale association should be exposed as null" ).isNull();
		session.delete( salesGuy );

		transaction.commit();
		session.close();
		checkCleanCache();
	}

	@Test
	public void testDefaultBiDirManyToOneCompositeKeyTest() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Court court = new Court();
		court.setId( new Court.CourtId() );
		court.getId().setCountryCode( "DE" );
		court.getId().setSequenceNo( 123 );
		court.setName( "Hamburg Court" );
		session.persist( court );
		Game game1 = new Game();
		game1.setId( new Game.GameId() );
		game1.getId().setCategory( "primary" );
		game1.getId().setSequenceNo( 456 );
		game1.setName( "The game" );
		game1.setPlayedOn( court );
		court.getGames().add( game1 );
		Game game2 = new Game();
		game2.setId( new Game.GameId() );
		game2.getId().setCategory( "primary" );
		game2.getId().setSequenceNo( 457 );
		game2.setName( "The other game" );
		game2.setPlayedOn( court );
		session.persist( game1 );
		session.persist( game2 );
		session.flush();
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Court localCourt = session.get( Court.class, new Court.CourtId( "DE", 123 ) );
		assertThat( localCourt.getGames() ).hasSize( 2 );
		for ( Game game : localCourt.getGames() ) {
			session.delete( game );
		}
		localCourt.getGames().clear();
		session.delete( localCourt );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				JUG.class,
				Member.class,
				SalesForce.class,
				SalesGuy.class,
				Beer.class,
				Brewery.class,
				Game.class,
				Court.class,
				Employee.class,
				Employer.class
		};
	}
}
