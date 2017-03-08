/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.embeddable.ForumUser;
import org.hibernate.ogm.backendtck.embeddable.JiraIssue;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1253")
public class ElementCollectionWithMapAndEmbeddedTest extends OgmTestCase {

	private ForumUser user;
	private JiraIssue ogmIssue;
	// we should also test the use case where all properties in JiraIssue are null

	@Before
	public void prepareDb() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			ogmIssue = new JiraIssue( 123, "OGM" );

			user = new ForumUser( "user123" );
			user.getIssues().put( "issue1", ogmIssue );
			user.getIssues().put( "issue2", ogmIssue );

			session.persist( user );
			tx.commit();
		}
	}

	@Test
	public void testMapping() throws Exception {
		assertDocument(
			getSessionFactory(),
			// collection
			ForumUser.LABEL,
			// query
			"{ '_id' : '" + user.getId() + "' }",
			// fields
			null,
			"{ '_id' : '" + user.getId() + "', 'issues' : { 'issue2' : { 'number' : 123, 'project' : 'OGM' }, 'issue1' : { 'number' : 123, 'project' : 'OGM' } } }"
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ ForumUser.class };
	}
}
