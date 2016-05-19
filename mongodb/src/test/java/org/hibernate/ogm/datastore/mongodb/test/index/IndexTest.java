/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Testing MongoDB Single Field Indexes
 *
 * @author Francois Le Droff
 */
public class IndexTest extends OgmTestCase {

	@Test
	public void testIndex() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		Poem barbara = new Poem("barbara", "Barbara", "Jacques Prevert");

		session.persist( barbara );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		/*
		 * assertDbObject( session.getSessionFactory(), // collection "TvShow",
		 * // query "{ '_id' : 'tvshow-1' }", // expected "{" +
		 * "'_id' : 'tvshow-1', " + "'episodes' : [ " +
		 * "{ 'idx' : 2, 'id' : 'episode-3'} ," +
		 * "{ 'idx' : 1, 'id' : 'episode-2'} ," +
		 * "{ 'idx' : 0, 'id' : 'episode-1'}" + "]," + "'name' : 'Baking Bread'"
		 * + "}" );
		 */

		// Clean-Up
		barbara = (Poem) session.get( Poem.class, "barbara" );
		session.delete( barbara );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class };
	}
}
