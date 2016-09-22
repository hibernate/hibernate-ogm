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
 * Test the execution of native queries on MongoDB using the {@link EntityManager}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBEntityManagerNativeQueryTest extends OgmJpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", Poem.class );

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", (byte) 5 );
	private final Poet christian = new Poet( "christian", "Christian Abendsonne" );
	private final Poet james = new Poet( "james", "James Krass" );
	private final LiteratureSociety stencilClub = new LiteratureSociety( "stencil-club", "Stencil Club Germany", christian, james );
	private final Critic critic = new Critic( new CriticId( "de", "764" ), "Roger" );

	private EntityManager em;

	@Before
	public void init() throws Exception {
		// prepare test data
		em = createEntityManager();
		begin();
		em = persist( portia, athanasia, stencilClub, critic );
		commit();
		em.close();

		em = createEntityManager();
	}

	@After
	public void tearDown() throws Exception {
		begin();
		delete( portia, athanasia, stencilClub, critic );
		commit();
		close( em );
	}

	@Test
	public void testSingleResultQuery() throws Exception {
		begin();

		String nativeQuery = "{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getSingleResult();

		assertAreEquals( portia, poem );

		commit();
	}

	@Test
	public void testSingleResultQueryWithProjection() throws Exception {
		begin();

		String nativeQuery = "db.WILDE_POEM.find( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1 }"
				+ " )";
		String result = (String) em.createNativeQuery( nativeQuery, "poemNameMapping" ).getSingleResult();

		assertThat( result ).isEqualTo( "Portia" );


		nativeQuery = "db.WILDE_POEM.findOne( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1 }"
				+ " )";
		result = (String) em.createNativeQuery( nativeQuery, "poemNameMapping" ).getSingleResult();

		assertThat( result ).isEqualTo( "Portia" );

		commit();
	}

	@Test
	public void testSingleResultQueryWithSeveralProjections() throws Exception {
		begin();

		String nativeQuery = "db.WILDE_POEM.find( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1, 'author' : 1 }"
				+ " )";
		Object[] result = (Object[]) em.createNativeQuery( nativeQuery, "poemNameAuthorIdMapping" ).getSingleResult();
		assertThat( Arrays.asList( result ) ).containsExactly( "Portia", "Oscar Wilde", 1L );

		nativeQuery = "db.WILDE_POEM.findOne( "
				+ "{ '$and' : [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] }, "
				+ "{ 'name' : 1, 'author' : 1 }"
				+ " )";
		result = (Object[]) em.createNativeQuery( nativeQuery, "poemNameAuthorIdMapping" ).getSingleResult();
		assertThat( Arrays.asList( result ) ).containsExactly( "Portia", "Oscar Wilde", 1L );

		@SuppressWarnings("unchecked")
		List<Object[]> results = em.createNativeQuery( nativeQuery, "poemNameAuthorIdMapping" ).getResultList();
		assertThat( results ).isNotNull();
		assertThat( results.size() ).isEqualTo( 1 );
		assertThat( Arrays.asList( results.get( 0 ) ) ).containsExactly( "Portia", "Oscar Wilde", 1L );

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
	// TODO OGM-564 Re-enable once HHH-8237 is resolved and we're on ORM 4.3.6
	public void testProjectionQueryWithTypeConversion() throws Exception {
		begin();

		List<Byte> result = em.createNamedQuery( "PoemRatings" ).getResultList();
		assertThat( result ).containsOnly( portia.getRating(), athanasia.getRating() );

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
	public void testExceptionWhenReturnedEntityIsMissing() throws Exception {
		begin();

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
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		begin();
		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, results.get( 0 ) );
		assertAreEquals( portia, results.get( 1 ) );

		commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-424")
	public void testEntitiesInsertedInCurrentSessionAreFoundByNativeQuery() throws Exception {
		begin();

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
	}

	@Test
	public void testSingleResultQueryUsingExtendedSyntax() throws Exception {
		begin();
		String nativeQuery = "db.WILDE_POEM.find({ '$query' : { 'name' : 'Athanasia' }, '$orderby' : { 'name' : 1 } })";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 1 );
		assertAreEquals( athanasia, results.get( 0 ) );

		commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-638")
	public void canRunSameNativeQuerySeveralTimes() throws Exception {
		begin();

		Query createNativeQuery = em.createNativeQuery(
				"db.LiteratureSociety.find( { 'name' : 'Stencil Club Germany' } )",
				LiteratureSociety.class
		);

		LiteratureSociety stencilClub = (LiteratureSociety) createNativeQuery.getSingleResult();
		assertThat( stencilClub.getMembers() ).onProperty( "id" ).containsOnly( "christian", "james" );

		stencilClub = (LiteratureSociety) createNativeQuery.getSingleResult();
		assertThat( stencilClub.getMembers() ).onProperty( "id" ).containsOnly( "christian", "james" );

		commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-702")
	public void testQueryWithCompositeId() throws Exception {
		begin();

		@SuppressWarnings("unchecked")
		List<Critic> critics = em.createNativeQuery( "{}", Critic.class ).getResultList();

		assertThat( critics ).onProperty( "id" ).containsExactly( new CriticId( "de", "764" ) );

		commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class, LiteratureSociety.class, Poet.class, Critic.class };
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
