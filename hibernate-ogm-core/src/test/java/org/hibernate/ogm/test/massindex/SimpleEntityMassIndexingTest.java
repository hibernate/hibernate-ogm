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
package org.hibernate.ogm.test.massindex;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.hsearch.Insurance;
import org.hibernate.ogm.test.id.NewsID;
import org.hibernate.ogm.test.massindex.model.IndexedLabel;
import org.hibernate.ogm.test.massindex.model.IndexedNews;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.junit.After;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class SimpleEntityMassIndexingTest extends OgmTestCase {

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
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<Insurance> list = session.createQuery( "FROM Insurance " ).list();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getName() ).isEqualTo( "Insurance Corporation" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	@Test
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
			Session session = openSession();
			Transaction transaction = session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<IndexedNews> list = session.createQuery( "FROM IndexedNews " ).list();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ).getContent() ).isEqualTo( "content" );
			assertThat( list.get( 0 ).getNewsId().getTitle() ).isEqualTo( "title" );
			assertThat( list.get( 0 ).getNewsId().getAuthor() ).isEqualTo( "author" );
			transaction.commit();
			session.clear();
			session.close();
		}
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		FileHelper.delete( getBaseIndexDir() );
	};

	private void startAndWaitMassIndexing(Class<?> entityType) throws InterruptedException {
		FullTextSession session = Search.getFullTextSession( openSession() );
		session.createIndexer( entityType ).purgeAllOnStart( true ).startAndWait();
	}

	private void purgeAll(Class<?> entityType) {
		FullTextSession session = Search.getFullTextSession( openSession() );
		session.purgeAll( entityType );
		session.flushToIndexes();
		@SuppressWarnings("unchecked")
		List<Insurance> list = session.createQuery( "FROM " + entityType.getSimpleName() ).list();
		assertThat( list ).hasSize( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Insurance.class, IndexedNews.class, IndexedLabel.class };
	}

	protected File getBaseIndexDir() {
		// Make sure no directory is ever reused across the testsuite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = this.getClass().getSimpleName() + "." + this.getName();

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		File indexPath = new File( TestConstants.getIndexDirectory(), shortTestName );
		return indexPath;
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		// Infinispan requires to be set to distribution mode for this test to pass
		cfg.setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-dist.xml" );
	}
}
