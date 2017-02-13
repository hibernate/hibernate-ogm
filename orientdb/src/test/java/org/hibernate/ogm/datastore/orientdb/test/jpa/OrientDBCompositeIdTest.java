/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.log4j.Logger;

import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.Passport;
import org.hibernate.ogm.datastore.orientdb.test.jpa.entity.PassportPK;

import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrientDBCompositeIdTest extends OgmJpaTestCase {

	private static final Logger log = Logger.getLogger( OrientDBCompositeIdTest.class.getName() );
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
		}
		em.clear();
	}

	@Test
	public void test1InsertNewPassport() {
		log.debug( "start" );
		try {
			em.getTransaction().begin();
			Passport newPassport = new Passport();
			newPassport.setFio( "fio1" );
			newPassport.setSeria( 6002 );
			newPassport.setNumber( 11111111 );
			log.debug( "New Passport ready for  persit" );
			em.persist( newPassport );
			em.getTransaction().commit();
			em.clear();
		}
		catch (Exception e) {
			log.error( "Error", e );
			em.getTransaction().rollback();
			throw e;
		}

		try {

			em.getTransaction().begin();
			PassportPK pk = new PassportPK();
			pk.setNumber( 11111111 );
			pk.setSeria( 6002 );
			Passport passport = em.find( Passport.class, pk );
			assertNotNull( "Passport must be saved!", passport );
			assertNotNull( "Passport must have a seria!", passport.getSeria() );
			assertEquals( "Seria must be a 6002", 6002, passport.getSeria() );
			assertEquals( "Seria must be a 11111111", 11111111, passport.getNumber() );
			assertEquals( "Seria must be a 'fio1'", "fio1", passport.getFio() );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			em.getTransaction().rollback();
			throw e;
		}

	}

	@Test
	public void test2UpdatePassport() {
		log.debug( "start" );
		PassportPK pk = new PassportPK();
		pk.setNumber( 11111111 );
		pk.setSeria( 6002 );
		try {

			em.getTransaction().begin();
			Passport passport = em.find( Passport.class, pk );
			assertNotNull( "Passport must be saved!", passport );
			assertNotNull( "Passport must have a seria!", passport.getSeria() );
			assertEquals( "Seria must be a 6002", 6002, passport.getSeria() );
			passport.setFio( "fio2" );
			em.merge( passport );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.clear();
		}
		try {
			em.getTransaction().begin();
			Passport passport = em.find( Passport.class, pk );
			assertNotNull( "Passport must be saved!", passport );
			assertNotNull( "Passport must have a seria!", passport.getSeria() );
			assertEquals( "Seria must be a 6002", 6002, passport.getSeria() );
			assertEquals( "Seria must be a 11111111", 11111111, passport.getNumber() );
			assertEquals( "Seria must be a 'fio2'", "fio2", passport.getFio() );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	// @Test
	public void test3SearchByNativeQuery() {
		log.debug( "start" );
		PassportPK pk1 = new PassportPK();
		pk1.setNumber( 11111111 );
		pk1.setSeria( 6002 );
		PassportPK pk2 = new PassportPK();
		pk2.setNumber( 22222222 );
		pk2.setSeria( 6002 );
		try {

			em.getTransaction().begin();
			Passport newPassport = new Passport();
			newPassport.setFio( "fio3" );
			newPassport.setSeria( pk2.getSeria() );
			newPassport.setNumber( pk2.getNumber() );
			log.debug( "New Passport ready for  persit" );
			em.persist( newPassport );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.clear();
		}
		try {
			em.getTransaction().begin();
			Query query = em.createNativeQuery( "select from Passport where fio=:fio", Passport.class );
			query.setParameter( "fio", "fio3" );
			@SuppressWarnings("unchecked")
			List<Passport> passports = query.getResultList();
			assertEquals( "Must be 2 passports!", 2, passports.size() );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	@Test
	public void test4RemovePassport() {
		log.debug( "start" );
		PassportPK pk = new PassportPK();
		pk.setNumber( 11111111 );
		pk.setSeria( 6002 );
		try {

			em.getTransaction().begin();
			Passport passport = em.find( Passport.class, pk );
			assertNotNull( "Passport must be saved!", passport );
			assertNotNull( "Passport must have a seria!", passport.getSeria() );
			assertEquals( "Seria must be a 6002", 6002, passport.getSeria() );
			em.remove( passport );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.clear();
		}
		try {
			em.getTransaction().begin();
			Passport passport = em.find( Passport.class, pk );
			assertNull( "Passport must be deleted!", passport );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Error", e );
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Passport.class, PassportPK.class };
	}

}
