/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Test case for Auto identifier generator using the session.
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 * @author Davide D'Alto
 */
public class AutoIdGeneratorWithSessionTest extends OgmTestCase {

	@Test
	public void testAutoIdentifierGenerator() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();

		DistributedRevisionControl git = new DistributedRevisionControl();
		git.setName( "Git" );
		session.persist( git );

		DistributedRevisionControl bzr = new DistributedRevisionControl();
		bzr.setName( "Bazaar" );
		session.persist( bzr );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		DistributedRevisionControl dvcs = (DistributedRevisionControl) session.get( DistributedRevisionControl.class, git.getId() );
		assertThat( dvcs ).isNotNull();
		assertThat( dvcs.getId() ).isEqualTo( 1 );
		session.delete( dvcs );

		dvcs = (DistributedRevisionControl) session.get( DistributedRevisionControl.class, bzr.getId() );
		assertThat( dvcs ).isNotNull();
		assertThat( dvcs.getId() ).isEqualTo( 2 );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DistributedRevisionControl.class };
	}
}
