/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.get;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
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
		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( expectedAssociations() );
		transaction.commit();
		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( expectedAssociations() );

		session.clear();

		transaction = session.beginTransaction();
		emmanuel = (Member) session.get( Member.class, emmanuel.getId() );
		jug = emmanuel.getMemberOf();
		session.delete( emmanuel );
		jerome = (Member) session.get( Member.class, jerome.getId() );
		session.delete( jerome );
		session.delete( jug );
		transaction.commit();
		assertThat( getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessions ) ).isEqualTo( 0 );

		session.close();

		checkCleanCache();
	}

	private Long expectedAssociations() {
		if ( TestHelper.getCurrentDialectType() == GridDialectType.NEO4J ) {
			// A relationship is created in Neo4j that will result in the count
			return 1L;
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
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
		assertNotNull( force.getSalesGuys() );
		assertEquals( 2, force.getSalesGuys().size() );
		simon = (SalesGuy) session.get( SalesGuy.class, simon.getId() );
		// purposely faulty
		// force.getSalesGuys().remove( simon );
		session.delete( simon );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
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
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
		assertEquals( 2, force.getSalesGuys().size() );
		SalesGuy salesGuy = (SalesGuy) session.get( SalesGuy.class, eric.getId() );
		salesGuy.setSalesForce( null );
		force.getSalesGuys().remove( salesGuy );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
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

		SalesGuy salesGuy = (SalesGuy) session.get( SalesGuy.class, "eric" );
		assertThat( salesGuy.getSalesForce() ).describedAs( "Stale association should be exposed as null" ).isNull();
		session.delete( salesGuy );

		salesGuy = (SalesGuy) session.get( SalesGuy.class, "simon" );
		assertThat( salesGuy.getSalesForce() ).describedAs( "Stale association should be exposed as null" ).isNull();
		session.delete( salesGuy );

		transaction.commit();
		session.close();
		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { JUG.class, Member.class, SalesForce.class, SalesGuy.class, Beer.class, Brewery.class };
	}
}
