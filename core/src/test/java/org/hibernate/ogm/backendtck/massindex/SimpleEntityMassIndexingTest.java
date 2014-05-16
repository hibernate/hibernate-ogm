/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.backendtck.massindex;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;

import java.io.File;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.hsearch.Insurance;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.backendtck.massindex.model.IndexedLabel;
import org.hibernate.ogm.backendtck.massindex.model.IndexedNews;
import org.hibernate.ogm.utils.IndexDirectoryManager;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class SimpleEntityMassIndexingTest extends OgmTestCase {

	private static final File indexDir = getBaseIndexDir();

	@Test
	public void testSimpleEntityMassIndexing() throws Exception {
		{
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			Insurance insurance = new Insurance();
			insurance.setName( "Insurance Corporation" );
			session.persist( insurance );
			transaction.commit();
			session.clear();
			session.close();
		}
		{
			purgeAll( Insurance.class );
			startAndWaitMassIndexing( Insurance.class );
		}
		{
			FullTextSession session = Search.getFullTextSession( openSession() );
			QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Insurance.class ).get();
			Query luceneQuery = queryBuilder.keyword().wildcard().onField( "name" ).matching( "ins*" ).createQuery();
			Transaction transaction = session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<Insurance> list = session.createFullTextQuery( luceneQuery ).list();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getName() ).isEqualTo( "Insurance Corporation" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Uses embedded key which is currently not supported by the db query parsers")
	public void testEntityWithCompositeIdMassIndexing() throws Exception {
		{
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			IndexedNews news = new IndexedNews( new NewsID( "title", "author" ), "content" );
			session.persist( news );
			transaction.commit();
			session.clear();
			session.close();
		}
		{
			purgeAll( IndexedNews.class );
			startAndWaitMassIndexing( IndexedNews.class );
		}
		{
			// Assert index creation
			FullTextSession session = Search.getFullTextSession( openSession() );
			QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( IndexedNews.class ).get();
			Query luceneQuery = queryBuilder.keyword().wildcard().onField( "newsId" ).ignoreFieldBridge().matching( "tit*" ).createQuery();
			Transaction transaction = session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<IndexedNews> list = session.createFullTextQuery( luceneQuery ).list();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getContent() ).isEqualTo( "content" );
			assertThat( list.get( 0 ).getNewsId().getTitle() ).isEqualTo( "title" );
			assertThat( list.get( 0 ).getNewsId().getAuthor() ).isEqualTo( "author" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	@AfterClass
	public static void deleteIndexDir() throws Exception {
		FileHelper.delete( indexDir );
	};

	private void startAndWaitMassIndexing(Class<?> entityType) throws InterruptedException {
		FullTextSession session = Search.getFullTextSession( openSession() );
		session.createIndexer( entityType ).purgeAllOnStart( true ).startAndWait();
		int numDocs = session.getSearchFactory().getIndexReaderAccessor().open( entityType ).numDocs();
		session.close();
		assertThat( numDocs ).isGreaterThan( 0 );
	}

	private void purgeAll(Class<?> entityType) {
		FullTextSession session = Search.getFullTextSession( openSession() );
		session.purgeAll( entityType );
		session.flushToIndexes();
		int numDocs = session.getSearchFactory().getIndexReaderAccessor().open( entityType ).numDocs();
		session.close();
		assertThat( numDocs ).isEqualTo( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class, IndexedNews.class, IndexedLabel.class };
	}

	protected static File getBaseIndexDir() {
		// Make sure no directory is ever reused across the testsuite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = SimpleEntityMassIndexingTest.class.getSimpleName() + "." + System.currentTimeMillis();

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		File indexPath = new File( IndexDirectoryManager.getIndexDirectory( SimpleEntityMassIndexingTest.class ), shortTestName );
		return indexPath;
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexBase", indexDir.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		// Infinispan requires to be set to distribution mode for this test to pass
		cfg.setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-dist.xml" );
	}
}
