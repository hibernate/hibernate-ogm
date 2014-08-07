/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.hibernate.ogm.backendtck.jpa.Poem;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the execution of native queries on MongoDB using the {@link EntityManager}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBEntityManagerNativeQueryTest extends JpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Poem.class );

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", (byte) 5 );

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
	public void testSingleResultQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getSingleResult();

		assertAreEquals( portia, poem );

		commit();
		close( em );
	}

	@Test
	public void testSingleResultQueryWithProjection() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "db.WILDE_POEM.find( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1 }"
				+ " )";
		String result = (String) em.createNativeQuery( nativeQuery, "poemNameMapping" ).getSingleResult();

		assertThat( result ).isEqualTo( "Portia" );

		commit();
		close( em );
	}

	@Test
	public void testSingleResultQueryWithSeveralProjections() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "db.WILDE_POEM.find( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1, 'author' : 1 }"
				+ " )";
		Object[] result = (Object[]) em.createNativeQuery( nativeQuery, "poemNameAuthorIdMapping" ).getSingleResult();

		assertThat( Arrays.asList( result ) ).containsExactly( "Portia", "Oscar Wilde", 1L );

		commit();
		close( em );
	}

	@Test
	public void testCountQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		Long result = (Long) em.createNamedQuery( "CountPoems" ).getSingleResult();
		assertThat( result ).isEqualTo( 2L );

		commit();
		close( em );
	}

	@Test
	public void testSingleResultFromNamedNativeQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQuery" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
		close( em );
	}

	@Test
	public void testSingleProjectionResultFromNamedNativeQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String result = (String) em.createNamedQuery( "AthanasiaProjectionQuery" ).getSingleResult();
		assertThat( result ).isEqualTo( athanasia.getName() );

		commit();
		close( em );
	}

	@Test
	@Ignore
	// TODO OGM-564 Re-enable once HHH-8237 is resolved and we're on ORM 4.3.6
	public void testProjectionQueryWithTypeConversion() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		List<Byte> result = em.createNamedQuery( "PoemRatings" ).getResultList();
		assertThat( result ).containsOnly( portia.getRating(), athanasia.getRating() );

		commit();
		close( em );
	}

	@Test
	public void testMappedEntityResultFromNamedNativeQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQueryWithMapping" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
		close( em );
	}


	@Test
	public void testExceptionWhenReturnedEntityIsMissing() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }";
		try {
			em.createNativeQuery( nativeQuery ).getSingleResult();
			commit();
		}
		catch (Exception he) {
			rollback();
			String message = he.getMessage();
			assertThat( message )
				.as( "The native query doesn't define a returned entity, there should be a specific exception" )
				.contains( "OGM001217" );
		}
		finally {
			close( em );
		}
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();
		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, results.get( 0 ) );
		assertAreEquals( portia, results.get( 1 ) );

		commit();
		close( em );
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-424")
	public void testEntitiesInsertedInCurrentSessionAreFoundByNativeQuery() throws Exception {
		begin();
		EntityManager em = createEntityManager();

		String nativeQuery = "{ name : 'Her Voice' }";
		Query query = em.createNativeQuery( nativeQuery, OscarWildePoem.class );

		List<OscarWildePoem> results = query.getResultList();
		assertThat( results ).as( "Unexpected number of results" ).hasSize( 0 );

		OscarWildePoem voice = new OscarWildePoem( 3L, "Her Voice", "Oscar Wilde" );
		em.persist( voice );

		results = query.getResultList();
		assertThat( results ).as( "Unexpected number of results" ).hasSize( 1 );

		assertAreEquals( voice, results.get( 0 ) );

		em.remove( voice );
		commit();
		close( em );
	}

	@Test
	public void testSingleResultQueryUsingExtendedSyntax() throws Exception {
		begin();
		EntityManager em = createEntityManager();
		String nativeQuery = "db.WILDE_POEM.find({ '$query' : { 'name' : 'Athanasia' }, '$orderby' : { 'name' : 1 } })";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 1 );
		assertAreEquals( athanasia, results.get( 0 ) );

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

	private void begin() throws NotSupportedException, SystemException, Exception {
		getTransactionManager().begin();
	}

	private void commit() throws Exception {
		getTransactionManager().commit();
	}

	private void rollback() throws Exception {
		getTransactionManager().rollback();
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

}
