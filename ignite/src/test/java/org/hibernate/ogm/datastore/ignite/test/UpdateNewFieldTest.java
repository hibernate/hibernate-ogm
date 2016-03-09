/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.test;

import org.hibernate.ogm.OgmSession;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateNewFieldTest extends BaseIgniteTest {

	private static final Logger log = LoggerFactory.getLogger( UpdateNewFieldTest.class );

	@Test
	@Ignore
	public void test() throws Exception {
		log.info( "==> test()" );
		OgmSession session = openSession();

		ObjectId id = new ObjectId(38, 1, 2);
		String name = "old name";
		String information = "old info";
		String newField = "new field";
		NewClient newClient = new NewClient(id, name, information, newField);

		testInsert( session, newClient );

		Client client = new Client(id.toString(), "new name", "new info");

		testUpdate( session, client );

		log.info( "<== test()" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Client.class, Deposit.class, NewClient.class};
	}

}
