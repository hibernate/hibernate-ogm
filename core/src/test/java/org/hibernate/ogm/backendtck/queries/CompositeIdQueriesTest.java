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
import static org.hibernate.ogm.utils.GridDialectType.REDIS_JSON;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_HASH;
import static org.hibernate.ogm.utils.SessionHelper.asProjectionResults;
import static org.hibernate.ogm.utils.SessionHelper.delete;
import static org.hibernate.ogm.utils.SessionHelper.persist;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.id.Label;
import org.hibernate.ogm.backendtck.id.News;
import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SessionHelper.ProjectionResult;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
@SkipByGridDialect(
	value = { CASSANDRA, COUCHDB, EHCACHE, HASHMAP, INFINISPAN, REDIS_JSON, REDIS_HASH },
	comment = "Hibernate Search does not store properties of the @EmbeddedId by default in the index, it requires the use of @FieldBridge."
			+ "It is also not sufficient to add a custom field bridge because the properties of the embedded id won't be recognized as properties of the entity."
			+ "There is a JIRA to keep track of this: OGM-849")
public class CompositeIdQueriesTest extends OgmTestCase {

	@TestSessionFactory
	public static SessionFactory sessions;

	private static final String author = "Guillaume";

	private static final String titleOGM = "How to use Hibernate OGM ?";
	private static final String titleAboutJUG = "What is a JUG ?";
	private static final String titleCountJUG = "There are more than 20 JUGs in France";

	private static final String contentOGM = "Simple, just like ORM but with a NoSQL database";
	private static final String contentAboutJUG = "JUG means Java User Group";
	private static final String contentCountJUG = "Great! Congratulations folks";

	private static final News aboutJUG = new News( new NewsID( titleAboutJUG, author ), contentAboutJUG, null );
	private static final News ogmHowTo = new News( new NewsID( titleOGM, author ), contentOGM, null );
	private static final News countJUG = new News( new NewsID( titleCountJUG, author ), contentCountJUG, null );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Session session;

	private Transaction tx;

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		persist( sessions, ogmHowTo, aboutJUG, countJUG );
	}

	@Test
	public void testQueryWithFileterOnAttribute() throws Exception {
		News result = (News) session.createQuery( "FROM News n WHERE n.newsId.title = '" + ogmHowTo.getNewsId().getTitle() + "'" ).uniqueResult();
		assertThat( result ).isEqualTo( ogmHowTo );
	}

	@Test
	public void testSingleAttributeProjection() throws Exception {
		String result = (String) session.createQuery( "SELECT n.newsId.title FROM News n WHERE n.newsId.title = '" + ogmHowTo.getNewsId().getTitle() + "'" ).uniqueResult();
		assertThat( result ).isEqualTo( ogmHowTo.getNewsId().getTitle() );
	}

	@Test
	public void testProjections() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "SELECT n.newsId.title, n.newsId.author, n.content FROM News n WHERE n.newsId.title = '" + ogmHowTo.getNewsId().getTitle() + "'" );
		assertThat( result ).containsExactly( new ProjectionResult( ogmHowTo.getNewsId().getTitle(), ogmHowTo.getNewsId().getAuthor(), ogmHowTo.getContent() ) );
	}

	@Before
	public void createSession() {
		closeSession();
		session = sessions.openSession();
		tx = session.beginTransaction();
	}

	@After
	public void closeSession() {
		if ( tx != null && tx.getStatus() == TransactionStatus.ACTIVE ) {
			tx.commit();
			tx = null;
		}
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@AfterClass
	public static void removeTestEntities() throws Exception {
		delete( sessions, News.class, ogmHowTo.getNewsId(), aboutJUG.getNewsId(), countJUG.getNewsId() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { News.class, Label.class };
	}
}
