package org.hibernate.ogm.datastore.infinispanremote.test.query;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemoteJPQLBookTest extends OgmTestCase {

	private final List<Book> allBooks = new ArrayList<>();

	private Book davideBook = new Book( "codeABC", "Davide", 2019, "Hiberante OGM in Action" );
	private Book andreaBook = new Book( "codeDEF", "Andrea", 2019, "Hiberante ORM: the definitive guide" );
	private Book sanneBook = new Book( "codeGHI", "Sanne", 2020, "Search & Analytics with Red Hat Data Grid" );
	private Book emmanuelBook = new Book( "codeLMN", "Emmanuel", 2021, "Data MicroServices with Red Hat Data Platform" );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void before() {
		allBooks.clear();

		inTransaction( session -> {
			List<Book> books = session.createQuery( "FROM Book", Book.class )
				.getResultList();

			assertThat( books ).isEmpty();
		} );

		inTransaction( session -> {
			session.persist( davideBook );
			session.persist( andreaBook );
			session.persist( sanneBook );
			session.persist( emmanuelBook );
		} );

		allBooks.add( davideBook );
		allBooks.add( andreaBook );
		allBooks.add( sanneBook );
		allBooks.add( emmanuelBook );
	}

	@After
	public void after() {
		inTransaction( session ->
			allBooks.forEach( book -> session.delete( book ) )
		);
		allBooks.clear();
	}

	@Test
	public void test_findAll() {
		final List<Book> queryResult = new ArrayList<>();

		inTransaction( session ->
			queryResult.addAll( session.createQuery( "SELECT b FROM Book b", Book.class )
				.getResultList() )
		);

		assertThat( queryResult ).containsOnly( davideBook, andreaBook, sanneBook, emmanuelBook );
	}

	@Test
	public void test_findByInteger() {
		final List<Book> queryResult = new ArrayList<>();

		inTransaction( session ->
			queryResult.addAll( session.createQuery( "FROM Book WHERE year = 2020", Book.class )
				.getResultList() )
		);

		assertThat( queryResult ).containsOnly( sanneBook );
	}

	@Test
	public void test_findByInteger_Withalias() {
		final List<Book> queryResult = new ArrayList<>();

		inTransaction( session ->
			queryResult.addAll( session.createQuery( "SELECT b FROM Book b WHERE b.year = 2019", Book.class )
				.getResultList() )
		);

		assertThat( queryResult ).containsOnly( davideBook, andreaBook );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Book.class };
	}
}
