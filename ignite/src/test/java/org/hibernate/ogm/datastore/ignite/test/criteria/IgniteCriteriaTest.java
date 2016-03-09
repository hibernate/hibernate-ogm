/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.test.criteria;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.ignite.test.BaseIgniteTest;
import org.hibernate.ogm.datastore.ignite.test.Client;
import org.hibernate.ogm.datastore.ignite.test.Deposit;
import org.hibernate.ogm.datastore.ignite.test.ObjectId;
import org.junit.Assert;
import org.junit.Test;

public class IgniteCriteriaTest extends BaseIgniteTest {

	@Test
	//@Ignore
	public void test() throws Exception {
		OgmSession session = openSession();
		ObjectId personId = new ObjectId(38, 1111, 2222);

		Client oldClient = (Client) session.get( Client.class, personId );
		if (oldClient != null) {
			testRemove( session, oldClient );
		}

		Client client = new Client(personId.toString(), "Name11111", "Client a-a-a-aaa");
		testInsert( session, client );

		String query = "SELECT NAME_ FROM Client WHERE ID_MEGA = :idMega AND NAME_ =  :name";

		SQLQuery sqlQuery = session.createNativeQuery( query ).addEntity( Client.class );
		sqlQuery.setInteger( "idMega", 38 );
		sqlQuery.setString( "name", "Name11111" );

		List<Client> list = sqlQuery.list();

		Assert.assertEquals( "Incorrect number of clients ", 1, list.size() );
	}

	@SuppressWarnings("unchecked")
	@Test
	//@Ignore
	public void testCriteria() throws Exception {
		OgmSession session = openSession();
		String personId = (new ObjectId(38, 2222, 3333)).toString();

		Client oldClient = (Client) session.get( Client.class, personId );
		if (oldClient != null) {
			testRemove( session, oldClient );
		}

		Client client = new Client(personId, "Criteria client", "Client for Criteria test");
		testInsert( session, client );

		Criteria criteria = session.createCriteria( Client.class );
		criteria.add( Restrictions.eq( "id", personId ) )
				.add( Restrictions.like( "name", "%client" ) );

		List<Client> list = criteria.list();

		Assert.assertTrue( "Incorrect number of clients ", list.size() > 0 );
	}

	@Test
	public void testQuery() throws Exception {
		OgmSession session = openSession();
		String personId = new ObjectId(38, 2222, 3333).toString();

		Client oldClient = (Client) session.get( Client.class, personId );
		if (oldClient != null) {
			testRemove( session, oldClient );
		}

		Client client = new Client(personId, "Criteria client", "Client for Criteria test");
		testInsert( session, client );

//		Criteria criteria = session.createCriteria(Client.class);
//		criteria.add(Restrictions.eq("id.megaId", 38))
//				.add(Restrictions.like("name", "%client"));
//
//		List<Client> list = criteria.list();
//		Assert.assertTrue("Incorrect number of clients ", list.size() > 0);

		Query q = session.createQuery( "from Client" );
		List result = q.list();
		System.out.println( result.size() );
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Client.class, Deposit.class};
	}

}
