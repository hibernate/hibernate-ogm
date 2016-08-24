/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.embeddable;

import static org.hibernate.ogm.datastore.redis.utils.RedisTestHelper.assertDbObject;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
@SkipByGridDialect(value = GridDialectType.REDIS_HASH, comment = "Redis Hash does not support embeddable associations")
public class ElementCollectionOfEmbeddableWithNamedColumnMappingTest extends OgmTestCase {

	private Address address1;
	private Address address2;
	private MultiAddressAccount account;

	@Before
	public void prepareDB() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			address1 = new Address();
			address1.setCity( "Paris" );
			address1.setZipCode( "75007" );

			address2 = new Address();
			address2.setCity( "Rome" );
			address2.setZipCode( "00184" );

			account = new MultiAddressAccount();
			account.setLogin( "gunnar" );
			account.getAddresses().add( address1 );
			account.getAddresses().add( address2 );

			session.persist( account );
			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-893")
	public void testMappingForElementCollectionWithNamedColumn() {
		assertDbObject(
				getSessionFactory(),
				// collection
				"MultiAddressAccount",
				// query
				account.getLogin(),
				// expected
				"{ " +
					"'addresses' : [" +
						"{ 'city' : '" + address1.getCity() + "', 'postal_code' : '" + address1.getZipCode() + "' }, " +
						"{ 'city' : '" + address2.getCity() + "', 'postal_code' : '" + address2.getZipCode() + "' }" +
					"]" +
				"}"
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MultiAddressAccount.class };
	}
}
