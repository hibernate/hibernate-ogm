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

import org.hibernate.ogm.backendtck.jpa.Poem;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the execution of native queries on Neo4j using the {@link EntityManager}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jEntityManagerNativeQueryTest extends OgmJpaTestCase {

	/*
	 *  The only purpose of this constant is to keep track of the nodes created using a native query via the
	 *	executeUpdate method. This is just a Label (not a table or entity).
	 */
	private static final String UPDATE_LABEL = "UPDATE";

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", Poem.class );

	private final Critic critic = new Critic( new CriticId( "de", "764" ), "Roger" );
	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", new GregorianCalendar( 1808, 3, 10, 12, 45 ).getTime(), critic );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", new GregorianCalendar( 1810, 3, 10 ).getTime(), null );

	private EntityManager em;

	@Before
	public void init() throws Exception {
		em = createEntityManager();
		persist( critic, portia, athanasia );
	}

	@After
	public void tearDown() throws Exception {
		delete( portia, athanasia, critic );
		em.close();
	}

	@Test
	public void testIteratorSingleResultQuery() throws Exception {
		em.getTransaction().begin();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:'Portia', author:'Oscar Wilde' } ) RETURN n";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getSingleResult();

		assertAreEquals( portia, poem );

		em.getTransaction().commit();
	}

	@Test
	public void testIteratorSingleResultFromNamedNativeQuery() throws Exception {
		em.getTransaction().begin();

		OscarWildePoem poem = (OscarWildePoem) em.createNamedQuery( "AthanasiaQuery" ).getSingleResult();

		assertAreEquals( athanasia, poem );

		em.getTransaction().commit();
	}



	@Test
	public void testListMultipleResultQuery() throws Exception {
		em.getTransaction().begin();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { author:'Oscar Wilde' } ) RETURN n ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> results = em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getResultList();

		assertThat( results ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, results.get( 0 ) );
		assertAreEquals( portia, results.get( 1 ) );

		em.getTransaction().commit();
	}

	@Test
	public void testSingleResultQueryUsingParameter() throws Exception {
		em.getTransaction().begin();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:{name}, author:'Oscar Wilde' } ) RETURN n";
		Query query = em.createNativeQuery( nativeQuery, OscarWildePoem.class );
		query.setParameter( "name", "Portia" );
		OscarWildePoem poem = (OscarWildePoem) query.getSingleResult();

		assertAreEquals( portia, poem );

		em.getTransaction().commit();
	}

	@Test
	public void testSingleResultQueryUsingDateParameter() throws Exception {
		em.getTransaction().begin();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { dateOfCreation:{creationDate}, author:'Oscar Wilde' } ) RETURN n";
		Query query = em.createNativeQuery( nativeQuery, OscarWildePoem.class );
		query.setParameter( "creationDate", new GregorianCalendar( 1810, 3, 10 ).getTime() );
		OscarWildePoem poem = (OscarWildePoem) query.getSingleResult();

		assertAreEquals( athanasia, poem );

		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-702")
	public void testQueryWithCompositeId() throws Exception {
		em.getTransaction().begin();

		@SuppressWarnings("unchecked")
		List<Critic> critics = em.createNativeQuery( "MATCH ( n:Critic ) RETURN n", Critic.class ).getResultList();

		assertThat( critics ).onProperty( "id" ).containsExactly( new CriticId( "de", "764" ) );

		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-854")
	public void testToOneInitializedByQuery() throws Exception {
		em.getTransaction().begin();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:'Portia', author:'Oscar Wilde' } ) RETURN n";
		OscarWildePoem poem = (OscarWildePoem) em.createNativeQuery( nativeQuery, OscarWildePoem.class ).getSingleResult();

		assertThat( poem.getCritic() ).isNotNull();
		assertThat( poem.getCritic().getName() ).as( "Wrong critic name" ).isEqualTo( "Roger" );

		em.getTransaction().commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNativeQueryExecuteUpdate() throws Exception {
		em.getTransaction().begin();

		String findQueryString = "MATCH (n:" + UPDATE_LABEL + ") RETURN n";
		Query findQuery = em.createNativeQuery( findQueryString );

		String createQuery = "CREATE (n:" + UPDATE_LABEL + " { author:'Giorgio Faletti' })";
		int updates = em.createNativeQuery( createQuery ).executeUpdate();
		if ( TestHelper.getCurrentDatastoreProviderType() != DatastoreProviderType.NEO4J_HTTP ) {
			assertThat( updates ).isEqualTo( 3 ); // 1 node + 1 label + 1 property set
		}
		List<Object> createdNode = (List<Object>) findQuery.getResultList();
		assertThat( createdNode ).hasSize( 1 );

		String deleteQuery = "MATCH (n:" + UPDATE_LABEL + ") DELETE n ";
		int deletes = em.createNativeQuery( deleteQuery ).executeUpdate();
		if ( TestHelper.getCurrentDatastoreProviderType() != DatastoreProviderType.NEO4J_HTTP ) {
			assertThat( deletes ).isEqualTo( 1 ); // 1 node
		}
		List<Object> uniqueResult = (List<Object>) findQuery.getResultList();
		assertThat( uniqueResult ).isEmpty();

		em.getTransaction().commit();
	}

	private void persist(Object... entities) {
		EntityManager em = createEntityManager();
		em.getTransaction().begin();
		for ( Object object : entities ) {
			em.persist( object );
		}
		em.getTransaction().commit();
		em.clear();
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class, Critic.class };
	}

	private void delete(Object... entities) {
		em.getTransaction().begin();
		for ( Object object : entities ) {
			Object entity = em.merge( object );
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.clear();
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

}
