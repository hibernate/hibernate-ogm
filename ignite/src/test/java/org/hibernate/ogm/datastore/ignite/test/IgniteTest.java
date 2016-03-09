/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.test;

import javax.cache.CacheException;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ogm.OgmSession;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class IgniteTest extends BaseIgniteTest {

	@Test
	public void test() throws Exception {
		OgmSession session = null;
		Client client = null;
		session = openSession();
		ObjectId personId = new ObjectId(38, 1111, 2222);
		client = new Client(personId.toString(), "Name11111", "Client a-a-aaa");

		testInsert( session, client );

		client = (Client) session.get( Client.class, client.getId(), new LockOptions(LockMode.PESSIMISTIC_READ) );

		client.setName( "!!!!!!!!!!!!!!" );

		try {
			testUpdateNewSession( client );
			Assert.fail( "Object is not lock" );
		}
		catch (CacheException e) {
			// all is good
			e.printStackTrace();
		}

		session.close();

	}

	@Test
	@Ignore
	public void testUpdateClient() throws Exception {
		OgmSession session = null;
		try {
			session = openSession();
			session.getTransaction().begin();
			ObjectId personId = new ObjectId(38, 1111, 2222);
			Client client = (Client) session.get( Client.class, personId, new LockOptions(LockMode.PESSIMISTIC_READ) );

			client.setName( "------------" );

			testUpdate( session, client );

			session.getTransaction().commit();
		}
		finally {
			if (session != null) {
				session.close();
			}
		}

	}

	@Test
	@Ignore
	public void testInsertAndDeleteClient() throws Exception {
		OgmSession session = openSession();
		ObjectId personId = new ObjectId(38, 1111, 2222);
		Client client = new Client(personId.toString(), "Name11111", "Client a-a-aaa");

		testInsert( session, client );

		Client loadedClient = (Client) testGet( Client.class, personId );
		Assert.assertNotNull( "Object not found ", loadedClient );
		Assert.assertEquals( "Found the wrong object ", loadedClient.getName(), client.getName() );

		testRemove( session, loadedClient );

		Client removedClient = (Client) testGet( Client.class, personId );
		Assert.assertNull( "The object was not removed", removedClient );

		session.close();
	}

	@Test
	@Ignore
	public void testGetClientByDeposit() throws Exception {
		OgmSession session = openSession();
		ObjectId personId = new ObjectId(38, 1111, 2222);
		Client client = new Client(personId.toString(), "Name11111", "Client a-a-aaa");

		testInsert( session, client );

		ObjectId depositId = new ObjectId(38, 3333, 4444);
		Deposit deposit = new Deposit(depositId, "42305810638172602317", personId);

		testInsert( session, deposit );

		Client clientByDeposit = (Client) testGet( Client.class, deposit.getPersonId() );
		Assert.assertNotNull( "Object not found ", clientByDeposit );
		Assert.assertEquals( "Found the wrong object ", clientByDeposit.getName(), client.getName() );
		session.close();
	}

	public Client testInsertClient(OgmSession session) throws Exception {
		ObjectId personId = new ObjectId(38, 1111, 2222);
		Client client = new Client(personId.toString(), "Name11111", "Client a-a-aaa");

		testInsert( session, client );

		Client loadedClient = (Client) testGet( Client.class, personId );
		Assert.assertNotNull( "Object not found ", loadedClient );
		Assert.assertEquals( "Found the wrong object ", loadedClient.getName(), client.getName() );

		return loadedClient;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Client.class, Deposit.class};
	}

}
