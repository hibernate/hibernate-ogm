/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.ogm.backendtck.jpa.Poem;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the execution of native queries on Cassandra using the {@link EntityManager}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Jonathan Halliday
 */
public class CassandraEntityManagerNativeQueryTest extends OgmJpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", Poem.class );

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", (byte) 2, new BigInteger( "10" ) );
	private final Critic critic = new Critic( new CriticId( "de", "764" ), "Roger" );

	private EntityManager em;

	@Before
	public void init() throws Exception {
		// prepare test data
		em = createEntityManager();
		begin();
		em = persist( portia, athanasia, critic );
		commit();
		em.close();

		em = createEntityManager();
	}

	@After
	public void tearDown() throws Exception {
		begin();
		delete( portia, athanasia, critic );
		commit();
		close( em );
	}

	@Test
	public void testSingleResultQuery() throws Exception {
		begin();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name='Portia'";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class )
				.getSingleResult();

		assertAreEquals( portia, poem );

		commit();
	}

	@Test
	public void testSingleResultQueryWithProjection() throws Exception {
		begin();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name='Portia'";
		String result = (String) em.createNativeQuery( nativeQuery, "poemNameMapping" ).getSingleResult();

		assertThat( result ).isEqualTo( "Portia" );

		commit();
	}

	@Test
	public void testSingleResultQueryWithSeveralProjections() throws Exception {
		begin();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name='Portia'";
		Object[] result = (Object[]) em.createNativeQuery( nativeQuery, "poemNameAuthorIdMapping" ).getSingleResult();

		assertThat( Arrays.asList( result ) ).containsExactly( "Portia", "Oscar Wilde", 1L );

		commit();
	}

	@Test
	public void testCountQuery() throws Exception {
		begin();

		Long result = (Long) em.createNamedQuery( "CountPoems" ).getSingleResult();
		assertThat( result ).isEqualTo( 2L );

		commit();
	}

	@Test
	public void testSingleResultFromNamedNativeQuery() throws Exception {
		begin();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQuery" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
	}

	@Test
	public void testSingleProjectionResultFromNamedNativeQuery() throws Exception {
		begin();

		String result = (String) em.createNamedQuery( "AthanasiaProjectionQuery" ).getSingleResult();
		assertThat( result ).isEqualTo( athanasia.getName() );

		commit();
	}

	@Test
	@Ignore
	// TODO OGM-564 Re-enable once HHH-9279 is resolved and we upgraded ORM
	public void testProjectionQueryWithTypeConversion() throws Exception {
		begin();

		List<Byte> result = em.createNamedQuery( "PoemRatings" ).getResultList();
		assertThat( result ).containsOnly( athanasia.getRating() );

		commit();
	}

	@Test
	public void testMappedEntityResultFromNamedNativeQuery() throws Exception {
		begin();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQueryWithMapping" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		commit();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		begin();
		String nativeQuery = "SELECT * FROM \"WILDE_POEM\"";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 2 );

		commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-424")
	public void testEntitiesInsertedInCurrentSessionAreFoundByNativeQuery() throws Exception {
		begin();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name='Her Voice'";
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
	}

	@Test
	@TestForIssue(jiraKey = "OGM-702")
	public void testQueryWithCompositeId() throws Exception {
		begin();

		@SuppressWarnings("unchecked")
		List<Critic> critics = em.createNativeQuery( "SELECT * FROM \"Critic\"", Critic.class ).getResultList();

		assertThat( critics ).onProperty( "id" ).containsExactly( new CriticId( "de", "764" ) );

		commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1008")
	@SuppressWarnings("unchecked")
	public void testQueryWithOrdinalParameters() throws Exception {
		begin();

		Query query = em.createNativeQuery( "SELECT * FROM \"WILDE_POEM\" WHERE name = ?" );
		query.setParameter( 1, "Portia" ); // CQL parameters positions start at 1

		List<OscarWildePoem> results = query.getResultList();
		assertThat( results ).as( "Unexpected number of results" ).hasSize( 1 );

		commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1008")
	@SuppressWarnings("unchecked")
	public void testQueryWithNamedParameters() throws Exception {
//		Named parameters do not work at all due to CassandraParameterMetadataBuilder's limitations.
//		See https://hibernate.atlassian.net/projects/OGM/issues/OGM-1008 for discussions about this issue.
		begin();

		try {
			Query query = em.createNativeQuery( "SELECT * \"WILDE_POEM\" WHERE name = :nameParam" );
			fail( "Named parameters should not work at all due to CassandraParameterMetadataBuilder's limitations (See OGM-1008)."
					+ "It seems the problem has been solved, well done. Delete this line, update the test accordingly and move on :)" );

			query.setParameter( "nameParam", "Portia" );
			List<OscarWildePoem> results = query.getResultList();
			assertThat( results ).as( "Unexpected number of results" ).hasSize( 1 );
		}
		catch (Exception e) {
			// Nothing to do, this feature is not working yet
		}

		commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {OscarWildePoem.class, Critic.class};
	}

	private EntityManager persist(Object... entities) {
		for ( Object object : entities ) {
			em.persist( object );
		}
		return em;
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	private void close(EntityManager em) {
		em.clear();
		em.close();
	}

	private EntityManager delete(Object... entities) {
		for ( Object object : entities ) {
			Object entity = em.merge( object );
			em.remove( entity );
		}
		return em;
	}

	private void begin() throws Exception {
		em.getTransaction().begin();
	}

	private void commit() throws Exception {
		em.getTransaction().commit();
	}

	private void rollback() throws Exception {
		em.getTransaction().rollback();
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

}
