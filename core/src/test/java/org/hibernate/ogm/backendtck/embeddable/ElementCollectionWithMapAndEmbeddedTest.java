/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class ElementCollectionWithMapAndEmbeddedTest extends OgmTestCase {

	private ForumUser user;
	private JiraIssue ogmIssue1;
	private JiraIssue ogmIssue2;
	private JiraIssue ogmIssueWithNull;

	@Before
	public void prepareDb() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			ogmIssue1 = new JiraIssue( 1, "OGM" );
			ogmIssue2 = new JiraIssue( 2, "OGM" );
			ogmIssueWithNull = new JiraIssue( null, null );

			user = new ForumUser( "Jane Doe" );
			user.getIssues().put( "issue1", ogmIssue1 );
			user.getIssues().put( "issue2", ogmIssue2 );
			user.getIssues().put( "issueWithNull", ogmIssueWithNull );

			session.persist( user );
			tx.commit();
		}
	}

	@Test
	public void testName() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			ForumUser actualUser = session.get( ForumUser.class, user.getId() );

			assertThat( actualUser.getIssues() )
					.includes(
							entry( "issue1", ogmIssue1 ),
							entry( "issue2", ogmIssue2 ) );

			// We don't expect it contains this element because all attributes of the embedded are null
			assertThat( actualUser.getIssues() )
					.excludes( entry( "issueWithNull", ogmIssueWithNull ) );

			tx.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ ForumUser.class };
	}
}
