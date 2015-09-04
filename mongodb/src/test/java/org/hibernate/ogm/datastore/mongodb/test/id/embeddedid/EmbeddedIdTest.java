/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.embeddedid;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author amorozov
 */
public class EmbeddedIdTest extends OgmTestCase {

	@Test
	public void listAllEntitiesWithEmbeddedId() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();
		session.persist( new Author( new AuthorId( "1" ), "Author #1" ) );
		session.persist( new Author( new AuthorId( "2" ), "Author #2" ) );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		Author secondAuthor = session.load( Author.class, new AuthorId( "2" ) );
		assertThat( secondAuthor ).describedAs( "Entity didn't loaded by its id" ).isNotNull();
		List authors = session.createQuery( "from Author" ).list();
		assertThat( authors.size() ).describedAs( "Query didn't yield expected result" ).isEqualTo( 2 );
		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Author.class };
	}

}
