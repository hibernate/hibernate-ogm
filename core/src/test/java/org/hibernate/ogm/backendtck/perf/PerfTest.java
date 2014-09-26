/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.perf;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Date;
import java.util.Random;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Ignore;

/**
 * Fire up jconsole and make sure the end of the populating phase does not go over the amount of RAM you
 * give your test VM. Typically 2.000.000 Authors fit in 2GB.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Ignore
public class PerfTest extends JpaTestCase {

	private static Random rand = new Random();

	public static void main(String[] args) {
		PerfTest perfTest = new PerfTest();
		try {
			perfTest.createFactory();
			// perfTest.testSimpleEntityInserts();
			// perfTest.testManyToOneAssociations();
			perfTest.testCollectionAssociations();
			perfTest.closeFactory();
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}

	}

	public void testCollectionAssociations() throws Exception {
		System.out.printf( "Warming up\n" );
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();
		int nbrOfAuthors = 5000; // 100;
		if ( nbrOfAuthors >= 200 ) {
			for ( int j = 0; j < nbrOfAuthors / 200; j++ ) {
				save200AuthorsAndCommit( em, 200 );
				save200BlogsAndCommit( em, 200 );
			}
		}
		else {
			save200AuthorsAndCommit( em, nbrOfAuthors );
			save200BlogsAndCommit( em, nbrOfAuthors );
		}
		getTransactionManager().commit();

		int nbrOfBlogEntries = 350000;
		System.out.printf( "Warm up period done\nSaving %s Blog entries\n", nbrOfBlogEntries );
		long start = System.nanoTime();
		getTransactionManager().begin();
		em.joinTransaction();
		for ( int j = 0; j < nbrOfBlogEntries / 200; j++ ) {
			save200BlogEntriesAndCommit( em, nbrOfAuthors, true );
		}
		getTransactionManager().commit();
		System.out.printf( "Writing %s took %sms ie %sns/entry\n", nbrOfBlogEntries,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / ( nbrOfBlogEntries ) );
		System.out.printf( "Collection ratio %s entries per collection\n", nbrOfBlogEntries / nbrOfAuthors );

		getTransactionManager().begin();
		em.joinTransaction();
		em = getFactory().createEntityManager();
		int nbr_of_reads = 100000;
		start = System.nanoTime();
		for ( int i = 0; i < nbr_of_reads; i++ ) {
			int primaryKey = rand.nextInt( nbrOfBlogEntries - 1 ) + 1; // start from 1
			BlogEntry blogEntry = em.find( BlogEntry.class, Long.valueOf( primaryKey ) );
			assertThat( blogEntry.getContent() ).isNotEmpty();
			assertThat( blogEntry.getId() ).isEqualTo( primaryKey );
			assertThat( blogEntry.getAuthor() ).isNotNull();
			assertThat( blogEntry.getAuthor().getFname() ).isNotEmpty();
			assertThat( blogEntry.getBlog().getTitle() ).isNotEmpty();
		}
		System.out.printf( "Reading %s took %sms ie %sns/entry\n", nbr_of_reads,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / ( nbr_of_reads ) );
		getTransactionManager().commit();
		em.clear();
		start = System.nanoTime();
		int blogReads = nbrOfAuthors * 10;
		if ( blogReads > 10000 ) {
			blogReads = 10000;
		}
		for ( int i = 0; i < blogReads; i++ ) {
			getTransactionManager().begin();
			em.joinTransaction();
			int primaryKey = randId( nbrOfAuthors );
			Blog blog = em.find( Blog.class, primaryKey );

			assertThat( blog.getTitle() ).isNotEmpty();
			assertThat( blog.getId() ).isEqualTo( primaryKey );
			assertThat( blog.getEntries() ).isNotNull();
			if ( blog.getEntries().size() < ( nbrOfBlogEntries / nbrOfAuthors ) / 10 ) {
				System.out.printf( "Small number of entries in this collection %s\n", blog.getEntries().size() );
			}
			em.clear();
			getTransactionManager().commit();
		}
		System.out.printf( "Reading from blog %s times took %sms ie %sns/entry\n", blogReads,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / blogReads );
		em.close();
	}

	public void testManyToOneAssociations() throws Exception {
		System.out.printf( "Warming up\n" );
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();
		int nbrOfAuthors = 50000;
		for ( int j = 0; j < nbrOfAuthors / 200; j++ ) {
			save200AuthorsAndCommit( em, 200 );
		}
		getTransactionManager().commit();

		int nbrOfBlogEntries = 350000;
		System.out.printf( "Warm up period done\nSaving %s Blog entries\n", nbrOfBlogEntries );
		long start = System.nanoTime();
		getTransactionManager().begin();
		em.joinTransaction();
		for ( int j = 0; j < nbrOfBlogEntries / 200; j++ ) {
			save200BlogEntriesAndCommit( em, nbrOfAuthors, false );
		}
		getTransactionManager().commit();
		System.out.printf( "Writing %s took %sms ie %sns/entry\n", nbrOfBlogEntries,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / ( nbrOfBlogEntries ) );

		getTransactionManager().begin();
		em.joinTransaction();
		em = getFactory().createEntityManager();
		int nbr_of_reads = 100000;
		start = System.nanoTime();
		for ( int i = 0; i < nbr_of_reads; i++ ) {
			int primaryKey = rand.nextInt( nbrOfBlogEntries - 1 ) + 1; // start from 1
			BlogEntry blog = em.find( BlogEntry.class, Long.valueOf( primaryKey ) );
			assertThat( blog.getContent() ).isNotEmpty();
			assertThat( blog.getId() ).isEqualTo( primaryKey );
			assertThat( blog.getAuthor() ).isNotNull();
			assertThat( blog.getAuthor().getFname() ).isNotEmpty();
		}
		System.out.printf( "Reading %s took %sms ie %sns/entry\n", nbr_of_reads,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / ( nbr_of_reads ) );

		em.close();
		getTransactionManager().commit();
	}

	public void testSimpleEntityInserts() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();
		int authors = 2000000;
		System.out.printf( "Warming up\n" );
		for ( int j = 0; j < 200; j++ ) {
			save200AuthorsAndCommit( em, 200 );
		}

		System.out.printf( "Warm up period done\nSaving %s entities\n", authors );
		long start = System.nanoTime();

		for ( int j = 0; j < authors / 200; j++ ) {
			save200AuthorsAndCommit( em, 200 );
		}
		System.out.printf( "Saving %s took %sms ie %sns/entry\n", authors, ( System.nanoTime() - start ) / 1000000,
				( System.nanoTime() - start ) / ( authors ) );
		em.close();
		getTransactionManager().commit();

		getTransactionManager().begin();
		em = getFactory().createEntityManager();
		int nbr_of_reads = 100000;
		start = System.nanoTime();
		for ( int i = 0; i < nbr_of_reads; i++ ) {
			int primaryKey = rand.nextInt( authors - 1 ) + 1; // start from 1
			Author author = em.find( Author.class, primaryKey );
			if ( author == null ) {
				System.out.printf( "Cannot find author %s, %sth loop\n", primaryKey, i );
			}
			else {
				assertThat( author.getBio() ).isNotEmpty();
				assertThat( author.getA_id() ).isEqualTo( primaryKey );
			}
		}
		System.out.printf( "Reading %s took %sms ie %sns/entry\n", nbr_of_reads,
				( System.nanoTime() - start ) / 1000000, ( System.nanoTime() - start ) / ( nbr_of_reads ) );

		em.close();
		getTransactionManager().commit();
	}

	private void save200AuthorsAndCommit(EntityManager em, int nbrOfAuthors) throws Exception {
		for ( int i = 0; i < nbrOfAuthors; i++ ) {
			Author author = new Author();
			author.setBio( "This is a decent size bio made of " + rand.nextDouble() + " stuffs" );
			author.setDob( new Date() );
			author.setFname( "Emmanuel " + rand.nextInt() );
			author.setLname( "Bernard " + rand.nextInt() );
			author.setMname( "" + rand.nextInt( 26 ) );
			em.persist( author );
		}
		em.flush();
		getTransactionManager().commit();
		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();
	}

	private void save200BlogsAndCommit(EntityManager em, int nbrFfBlogs) throws Exception {
		for ( int i = 0; i < nbrFfBlogs; i++ ) {
			Blog blog = new Blog();
			blog.setTitle( "My life in a blog" + rand.nextDouble() );
			blog.setDescription( "I will describe what's happening in my life and ho things are going "
					+ rand.nextDouble() );
			em.persist( blog );
		}
		em.flush();
		getTransactionManager().commit();
		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();
	}

	private void save200BlogEntriesAndCommit(EntityManager em, int nbrOfAuthors, boolean alsoAddBlog) throws Exception {
		for ( int i = 0; i < 200; i++ ) {
			BlogEntry blogEntry = new BlogEntry();
			blogEntry.setTitle( "This is a blog of " + rand.nextDouble() );
			blogEntry
					.setContent( "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas bibendum risus commodo purus pellentesque quis venenatis enim tincidunt. Maecenas at nisl in nunc eleifend rutrum eu sit amet urna. Nulla dui diam, mollis a facilisis nec, iaculis feugiat lectus. Donec egestas, dui id facilisis euismod, lorem dui ornare est, vel feugiat ipsum odio et augue. Phasellus laoreet quam et augue hendrerit interdum cursus urna sodales. Cras eleifend mollis pharetra. Donec lectus sapien, ultricies eu fermentum sed, tempor nec odio.\n"
							+ "\n"
							+ rand.nextDouble()
							+ "Proin ullamcorper bibendum leo, ut luctus turpis sodales nec. Sed diam augue, malesuada quis dapibus eu, convallis et ligula. Duis eget vehicula quam. Quisque id mauris non nisl mattis tempus a non augue. In ut purus orci, vitae eleifend ipsum. Praesent convallis fringilla massa non tincidunt. Duis eget erat venenatis purus iaculis accumsan eu et mauris. Mauris id risus et erat consequat eleifend vitae in nisl. Integer consequat, velit vel dapibus posuere, nisl magna semper tellus, ut commodo felis purus at ipsum. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas.\n"
							+ "\n"
							+ "Ut sodales purus sit amet sapien semper sagittis. Duis aliquam tempus dictum. Cras suscipit ullamcorper cursus. Nam mollis lacinia aliquam. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Maecenas quis risus est, sit amet iaculis sapien. Ut nibh sapien, ornare ac mattis et, scelerisque eu leo. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas.\n"
							+ "\n"
							+ "Nulla in elit in felis viverra venenatis id a ligula. Nunc nec odio felis, vel ultricies metus. Morbi placerat porta elementum. Vestibulum a lacinia lectus. Nunc mauris nunc, luctus non mattis ac, venenatis vitae nulla. Sed risus est, imperdiet vitae molestie in, ullamcorper nec urna. Donec eu risus sem. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nulla." );
			int randId = randId( nbrOfAuthors );
			blogEntry.setAuthor( em.find( Author.class, randId ) );
			if ( alsoAddBlog ) {
				randId = randId( nbrOfAuthors );
				blogEntry.setBlog( em.find( Blog.class, randId ) );
			}
			em.persist( blogEntry );

			// stuff in or out of loop
			// em.flush();
			getTransactionManager().commit();
			em.clear();
			getTransactionManager().begin();
			em.joinTransaction();
		}

	}

	private int randId(int nbrOfAuthors) {
		int randId;
		randId = nbrOfAuthors > 1 ? rand.nextInt( nbrOfAuthors - 1 ) + 1 : 1;
		return randId;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Author.class, BlogEntry.class, Blog.class };
	}
}
