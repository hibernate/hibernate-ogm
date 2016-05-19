/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.CASSANDRA;
import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_HASH;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_JSON;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@SkipByGridDialect(
		value = { CASSANDRA, COUCHDB, EHCACHE, HASHMAP, INFINISPAN, REDIS_JSON, REDIS_HASH, NEO4J },
		comment = "We need a QueryParserService to be able to perform these queries.")
public class QueriesWithAssociationsTest extends JpaTestCase {

	private EntityManager em;

	@Test
	@SuppressWarnings("unchecked")
	public void testGetWithObjectComparison() throws Exception {
		Author alma = (Author) em.createQuery( "FROM Author WHERE name = :name" )
				.setParameter( "name", "alma" )
				.getSingleResult();

		Author alfred = (Author) em.createQuery( "FROM Author WHERE name = :name" )
				.setParameter( "name", "alfred" )
				.getSingleResult();

		Author alphonse = (Author) em.createQuery( "FROM Author WHERE name = :name" )
				.setParameter( "name", "alphonse" )
				.getSingleResult();

		List<Hypothesis> hypothesis;

		hypothesis = em.createQuery( "FROM Hypothesis WHERE author = :author" )
				.setParameter( "author", alma )
				.getResultList();

		assertThat( hypothesis.size() ).isEqualTo( 1 );
		assertThat( hypothesis.get( 0 ).getAuthor() ).isEqualTo( alma );

		hypothesis = em.createQuery( "FROM Hypothesis WHERE author = :author" )
				.setParameter( "author", alfred )
				.getResultList();

		assertThat( hypothesis.size() ).isEqualTo( 1 );
		assertThat( hypothesis.get( 0 ).getAuthor() ).isEqualTo( alfred );

		hypothesis = em.createQuery( "FROM Hypothesis WHERE author = :author" )
				.setParameter( "author", alphonse )
				.getResultList();

		assertThat( hypothesis.size() ).isEqualTo( 0 );

		hypothesis = em.createQuery( "FROM Hypothesis WHERE author IN(:authors)" )
				.setParameter( "authors", Arrays.asList( alma, alfred ) )
				.getResultList();

		assertThat( hypothesis.size() ).isEqualTo( 2 );
		assertThat( hypothesis ).onProperty( "author" ).containsOnly( alma, alfred );


	}

	@Before
	public void populateDb() throws Exception {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Address mainStreet = new Address();
		mainStreet.setId( 1L );
		mainStreet.setCity( "London" );
		mainStreet.setStreet( "Main Street" );
		em.persist( mainStreet );

		Address garibaldiStreet = new Address();
		garibaldiStreet.setId( 2L );
		garibaldiStreet.setCity( "Lyon" );
		garibaldiStreet.setStreet( "rue Garibaldi" );
		em.persist( garibaldiStreet );

		Author alfred = new Author();
		alfred.setId( 1L );
		alfred.setName( "alfred" );
		alfred.setAddress( mainStreet );
		em.persist( alfred );

		Author alma = new Author();
		alma.setId( 2L );
		alma.setName( "alma" );
		alma.setAddress( garibaldiStreet );
		em.persist( alma );

		Author alphonse = new Author();
		alphonse.setId( 3L );
		alphonse.setName( "alphonse" );
		alphonse.setAddress( mainStreet );
		em.persist( alphonse );

		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.set( 2012, 8, 25 );

		Hypothesis socrates = new Hypothesis();
		socrates.setId( "13" );
		socrates.setDescription( "There are more than two dimensions over the shadows we see out of the cave" );
		socrates.setPosition( 1 );
		socrates.setDate( calendar.getTime() );
		em.persist( socrates );

		calendar.set( Calendar.YEAR, 2011 );
		Hypothesis peano = new Hypothesis();
		peano.setId( "14" );
		peano.setDescription( "Peano's curve and then Hilbert's space filling curve proof the connection from mono-dimensional to bi-dimensional space" );
		peano.setPosition( 2 );
		peano.setDate( calendar.getTime() );
		peano.setAuthor( alma );
		em.persist( peano );

		calendar.set( Calendar.YEAR, 2010 );
		Hypothesis sanne = new Hypothesis();
		sanne.setId( "15" );
		sanne.setDescription( "Hilbert's proof of connection to 2 dimensions can be induced to reason on N dimensions" );
		sanne.setPosition( 3 );
		sanne.setDate( calendar.getTime() );
		em.persist( sanne );

		calendar.set( Calendar.YEAR, 2009 );
		Hypothesis shortOne = new Hypothesis();
		shortOne.setId( "16" );
		shortOne.setDescription( "stuff works" );
		shortOne.setPosition( 4 );
		shortOne.setDate( calendar.getTime() );
		shortOne.setAuthor( alfred );
		em.persist( shortOne );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
	}

	@After
	public void closeEmAndRemoveEntities() throws Exception {
		//Do not hide the real cause with an NPE if there are initialization issues:
		if ( em != null ) {
			em.getTransaction().commit();
			removeEntities();
			em.close();
		}
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Address.class, Author.class, Hypothesis.class };
	}

	private void removeEntities() throws Exception {
		em.getTransaction().begin();
		for ( Class<?> each : getEntities() ) {
			List<?> entities = em.createQuery( "FROM " + each.getSimpleName() ).getResultList();
			for ( Object object : entities ) {
				em.remove( object );
			}
		}
		em.getTransaction().commit();
	}

}
