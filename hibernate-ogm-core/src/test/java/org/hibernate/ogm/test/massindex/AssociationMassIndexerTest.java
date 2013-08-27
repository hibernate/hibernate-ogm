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
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.fest.util.Files;
import org.hibernate.Session;
import org.hibernate.ogm.test.hsearch.Insurance;
import org.hibernate.ogm.test.id.NewsID;
import org.hibernate.ogm.test.massindex.model.IndexedLabel;
import org.hibernate.ogm.test.massindex.model.IndexedNews;
import org.hibernate.ogm.test.utils.GridDialectType;
import org.hibernate.ogm.test.utils.IndexDirectoryManager;
import org.hibernate.ogm.test.utils.SkipByGridDialect;
import org.hibernate.ogm.test.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.test.utils.jpa.JpaTestCase;
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
