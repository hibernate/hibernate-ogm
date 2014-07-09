/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.query.nativequery.OscarWildePoem.TABLE_NAME;

import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.hibernate.ogm.backendtck.jpa.Poem;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the execution of native queries on Neo4j using the {@link EntityManager}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jEntityManagerNativeQueryTest extends JpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Poem.class );

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", new GregorianCalendar( 1808, 3, 10, 12, 45 ).getTime() );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", new GregorianCalendar( 1810, 3, 10 ).getTime() );

	@Before
	public void init() throws Exception {
		begin();
		EntityManager em = persist( portia, athanasia );
		commit();
		close( em );
	}

	private EntityManager persist(Object... entities) {
		EntityManager em = createEntityManager();
		for ( Object object : entities ) {
			em.persist( object );
		}
		return em;
	}

	@After
	public void tearDown() throws Exception {
		begin();
		EntityManager em = delete( portia, athanasia );
		commit();
		close( em );
	}

	@Test
	public void testIteratorSingleResultQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:'Portia', author:'Oscar Wilde' } ) RETURN n";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getSingleResult();

		assertAreEquals( portia, poem );

		commit();
		close( em );
	}

	@Test
	public void testIteratorSingleResultFromNamedNativeQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQuery" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
		close( em );
	}

	private void begin() throws NotSupportedException, SystemException, Exception {
		getTransactionManager().begin();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();
		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { author:'Oscar Wilde' } ) RETURN n ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, results.get( 0 ) );
		assertAreEquals( portia, results.get( 1 ) );

		commit();
		close( em );
	}

	@Test
	public void testSingleResultQueryUsingParameter() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:{name}, author:'Oscar Wilde' } ) RETURN n";
		Query query = em.createNativeQuery( nativeQuery, OscarWildePoem.class );
		query.setParameter( "name", "Portia" );
		OscarWildePoem poem = (OscarWildePoem) query.getSingleResult();

		assertAreEquals( portia, poem );

		commit();
		close( em );
	}

	@Test
	public void testSingleResultQueryUsingDateParameter() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { dateOfCreation:{creationDate}, author:'Oscar Wilde' } ) RETURN n";
		Query query = em.createNativeQuery( nativeQuery, OscarWildePoem.class );
		query.setParameter( "creationDate", new GregorianCalendar( 1810, 3, 10 ).getTime() );
		OscarWildePoem poem = (OscarWildePoem) query.getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
		close( em );
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { OscarWildePoem.class };
	}

	private void close(EntityManager em) {
		em.clear();
		em.close();
	}

	private EntityManager delete(Object... entities) {
		EntityManager em = createEntityManager();
		for ( Object object : entities ) {
			em.detach( object );
		}
		return em;
	}

	private void commit() throws Exception {
		getTransactionManager().commit();
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

}
