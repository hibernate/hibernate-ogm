/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.parameters;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Gunnar Morling
 *
 */
public class QueryWithParametersTest extends OgmJpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", Movie.class );

	@Test
	public void canUseByteForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.viewerRating = 8", Movie.class )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "To thatch a roof" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseByteAsParameterForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.viewerRating = :viewerRating", Movie.class )
			.setParameter( "viewerRating", (byte) 8 )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "To thatch a roof" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseByteAsParameterForInComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.viewerRating IN (:viewerRating)", Movie.class )
			.setParameter( "viewerRating", Arrays.asList( (byte) 8, (byte) 9 ) )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "To thatch a roof", "South by Southeast" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@Ignore("TODO HQLPARSER-59")
	public void canUseEnumLiteralForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.genre = org.hibernate.ogm.backendtck.queries.enums.Genre.THRILLER", Movie.class )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "South by Southeast", "Front Door" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseEnumAsParameterForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.genre = :genre", Movie.class )
			.setParameter( "genre", Genre.THRILLER )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "South by Southeast", "Front Door" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseQueriesWithEnumAsParameterForInQuery() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.genre IN (:genre)", Movie.class )
			.setParameter( "genre", EnumSet.of( Genre.DRAMA, Genre.COMEDY ) )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "To thatch a roof", "Barnie" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@Ignore("TODO HQLPARSER-59")
	public void canUseBooleanLiteralForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.suitableForKids = FALSE", Movie.class )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "Front Door", "Barnie" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseBooleanAsParameterForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.suitableForKids = :suitable", Movie.class )
			.setParameter( "suitable", Boolean.FALSE )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "Front Door", "Barnie" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	@Ignore("TODO HQLPARSER-59")
	public void canUseDateLiteralForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.releaseDate = '02 April 1958'", Movie.class )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "South by Southeast" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Test
	public void canUseDateParameterForSimpleComparison() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		List<Movie> thrillers = entityManager.createQuery( "SELECT m FROM Movie m WHERE m.releaseDate = :releaseDate", Movie.class )
			.setParameter( "releaseDate", new GregorianCalendar( 1958, 3, 2 ).getTime() )
			.getResultList();

		assertThat( thrillers ).onProperty( "title" ).containsOnly( "South by Southeast" );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Before
	public void insertTestEntities() throws Exception {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		entityManager.persist( new Movie( "movie-1", Genre.COMEDY, "To thatch a roof", true, new GregorianCalendar( 1955, 5, 10 ).getTime(), (byte) 8 ) );
		entityManager.persist( new Movie( "movie-2", Genre.THRILLER, "South by Southeast", true, new GregorianCalendar( 1958, 3, 2 ).getTime(), (byte) 9 ) );
		entityManager.persist( new Movie( "movie-3", Genre.THRILLER, "Front Door", false, new GregorianCalendar( 1961, 2, 23 ).getTime(), (byte) 7 ) );
		entityManager.persist( new Movie( "movie-4", Genre.DRAMA, "Barnie", false, new GregorianCalendar( 1962, 11, 2 ).getTime(), (byte) 7 ) );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@After
	public void removeTestEntities() {
		EntityManager entityManager = getFactory().createEntityManager();
		entityManager.getTransaction().begin();

		entityManager.remove( entityManager.find( Movie.class, "movie-1" ) );
		entityManager.remove( entityManager.find( Movie.class, "movie-2" ) );
		entityManager.remove( entityManager.find( Movie.class, "movie-3" ) );
		entityManager.remove( entityManager.find( Movie.class, "movie-4" ) );

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Movie.class };
	}
}
