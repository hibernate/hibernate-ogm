/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.fest.util.Files;
import org.hibernate.Session;
import org.hibernate.ogm.backendtck.hsearch.Insurance;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.IndexDirectoryManager;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.After;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationMassIndexerTest extends JpaTestCase {

	private File baseDir;

	@Test
	@SkipByGridDialect(value = GridDialectType.MONGODB, comment = "Uses embedded key which is currently not supported by the MongoDB query parser")
	public void testEntityWithAssociationMassIndexing() throws Exception {
		{
			List<IndexedLabel> labes = Arrays.asList( new IndexedLabel( "massindex" ), new IndexedLabel( "test" ) );
			IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
			news.setLabels( labes );
			boolean operationSuccessful = false;
			try {
				getTransactionManager().begin();
				EntityManager em = createEntityManager();
				em.persist( news );
				operationSuccessful = true;
			}
			finally {
				commitOrRollback( operationSuccessful );
			}
		}
		{
			purgeAll( IndexedNews.class );
			purgeAll( IndexedLabel.class );
			startAndWaitMassIndexing( IndexedNews.class, IndexedLabel.class );
		}
		{
			boolean operationSuccessful = false;
			try {
				getTransactionManager().begin();
				@SuppressWarnings("unchecked")
				List<IndexedNews> list = createSession().createQuery( "FROM IndexedNews " ).list();
				assertThat( list ).hasSize( 1 );

				List<IndexedLabel> labels = list.get( 0 ).getLabels();
				assertThat( labels ).hasSize( 2 );
				assertThat( contains( labels, "massindex" ) ).isTrue();
				assertThat( contains( labels, "test" ) ).isTrue();
				operationSuccessful = true;
			}
			finally {
				commitOrRollback( operationSuccessful );
			}
		}
		{
			boolean operationSuccessful = false;
			try {
				getTransactionManager().begin();
				@SuppressWarnings("unchecked")
				List<IndexedLabel> labels = createSession().createQuery( "FROM IndexedLabel " ).list();
				assertThat( labels ).hasSize( 2 );
				assertThat( contains( labels, "massindex" ) ).isTrue();
				assertThat( contains( labels, "test" ) ).isTrue();
				operationSuccessful = true;
			}
			finally {
				commitOrRollback( operationSuccessful );
			}
		}
	}

	private boolean contains(List<IndexedLabel> list, String label) {
		for ( IndexedLabel indexedLabel : list ) {
			if ( indexedLabel.getName().equals( label ) ) {
				return true;
			}
		}
		return false;
	}

	@After
	public void tearDown() throws Exception {
		super.closeFactory();
		Files.delete( baseDir );
	};

	private EntityManager createEntityManager() {
		return getFactory().createEntityManager();
	}

	private void startAndWaitMassIndexing(Class<?>... entityTypes) throws InterruptedException {
		FullTextSession session = Search.getFullTextSession( createSession() );
		session.createIndexer( entityTypes ).purgeAllOnStart( true ).startAndWait();
	}

	private Session createSession() {
		return (Session) createEntityManager().getDelegate();
	}

	private void purgeAll(Class<?>... entityTypes) {
		FullTextSession session = Search.getFullTextSession( createSession() );
		for ( Class<?> entityType : entityTypes ) {
			session.purgeAll( entityType );
			session.flushToIndexes();
			@SuppressWarnings("unchecked")
			List<Insurance> list = session.createQuery( "FROM " + entityType.getSimpleName() ).list();
			assertThat( list ).hasSize( 0 );
		}
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { IndexedNews.class, IndexedLabel.class };
	}

	protected File getBaseIndexDir() {
		// Make sure no directory is ever reused across the testsuite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = this.getClass().getSimpleName() + "." + Math.random();

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		baseDir = new File( IndexDirectoryManager.getIndexDirectory( AssociationMassIndexerTest.class ), shortTestName );
		return baseDir;
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties().setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		info.getProperties().setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		// Infinispan requires to be set to distribution mode for this test to pass
		info.getProperties().setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-dist.xml" );
	}
}
