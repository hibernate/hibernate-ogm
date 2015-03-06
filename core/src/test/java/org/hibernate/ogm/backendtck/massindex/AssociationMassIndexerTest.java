/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.fest.util.Files;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.IndexDirectoryManager;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.After;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class AssociationMassIndexerTest extends JpaTestCase {

	private static final File baseDir = getBaseIndexDir();

	@After
	public void tearDown() throws Exception {
		super.closeFactory();
		Files.delete( baseDir );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Uses embedded key which is currently not supported by the db query parsers")
	public void testEntityWithAssociationMassIndexing() throws Exception {
		populateDatastore();
		purgeAll( IndexedNews.class, IndexedLabel.class );
		startAndWaitMassIndexing( IndexedNews.class, IndexedLabel.class );

		assertEntityHasBeenIndexed();
		assertAssociatedElementsHaveBeenIndexed();
	}

	private void populateDatastore() throws Exception {
		List<IndexedLabel> labes = Arrays.asList( new IndexedLabel( "massindex" ), new IndexedLabel( "test" ) );
		IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
		news.setLabels( labes );
		EntityManager em = createEntityManager();
		em.getTransaction().begin();
		em.persist( news );
		em.getTransaction().commit();
		em.close();
	}

	private void assertEntityHasBeenIndexed() throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.getTransaction().begin();
		QueryBuilder queryBuilder = fullTextEm.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( IndexedNews.class )
				.get();
		Query luceneQuery = queryBuilder.keyword().wildcard().onField( "newsId" ).ignoreFieldBridge().matching(
				"tit*"
		).createQuery();
		@SuppressWarnings("unchecked")
		List<IndexedNews> list = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
		assertThat( list ).hasSize( 1 );

		List<IndexedLabel> labels = list.get( 0 ).getLabels();
		assertThat( labels ).hasSize( 2 );
		assertThat( contains( labels, "massindex" ) ).isTrue();
		assertThat( contains( labels, "test" ) ).isTrue();
		fullTextEm.getTransaction().commit();
		fullTextEm.close();
	}

	@SuppressWarnings("unchecked")
	private void assertAssociatedElementsHaveBeenIndexed() throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.getTransaction().begin();
		QueryBuilder b = fullTextEm.getSearchFactory().buildQueryBuilder().forEntity( IndexedLabel.class ).get();
		{
			Query luceneQuery = b.keyword().wildcard().onField( "name" ).matching( "tes*" ).createQuery();
			List<IndexedLabel> labels = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "test" ) ).isTrue();
		}
		{
			Query luceneQuery = b.keyword().wildcard().onField( "name" ).matching( "mas*" ).createQuery();
			List<IndexedLabel> labels = fullTextEm.createFullTextQuery( luceneQuery ).getResultList();
			assertThat( labels ).hasSize( 1 );
			assertThat( contains( labels, "massindex" ) ).isTrue();
		}
		fullTextEm.getTransaction().commit();
		fullTextEm.close();
	}

	private boolean contains(List<IndexedLabel> list, String label) {
		for ( IndexedLabel indexedLabel : list ) {
			if ( indexedLabel.getName().equals( label ) ) {
				return true;
			}
		}
		return false;
	}

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

	private void startAndWaitMassIndexing(Class<?>... entityTypes) throws InterruptedException {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		fullTextEm.createIndexer( entityTypes ).purgeAllOnStart( true ).startAndWait();
		int numDocs = fullTextEm.getSearchFactory().getIndexReaderAccessor().open( entityTypes ).numDocs();
		fullTextEm.close();
		assertThat( numDocs ).isGreaterThan( 0 );
	}

	private void purgeAll(Class<?>... entityTypes) throws Exception {
		FullTextEntityManager fullTextEm = Search.getFullTextEntityManager( createEntityManager() );
		for ( Class<?> entityType : entityTypes ) {
			fullTextEm.purgeAll( entityType );
			fullTextEm.flushToIndexes();
		}
		int numDocs = fullTextEm.getSearchFactory().getIndexReaderAccessor().open( entityTypes ).numDocs();
		fullTextEm.close();
		assertThat( numDocs ).isEqualTo( 0 );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { IndexedNews.class, IndexedLabel.class };
	}

	protected static File getBaseIndexDir() {
		// Make sure no directory is ever reused across the testsuite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = AssociationMassIndexerTest.class.getSimpleName() + "." + Math.random();

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		return new File( IndexDirectoryManager.getIndexDirectory( AssociationMassIndexerTest.class ), shortTestName );
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties().setProperty( "hibernate.search.default.indexBase", baseDir.getAbsolutePath() );
		info.getProperties().setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		// Infinispan requires to be set to distribution mode for this test to pass
		info.getProperties().setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-dist.xml" );
	}
}
