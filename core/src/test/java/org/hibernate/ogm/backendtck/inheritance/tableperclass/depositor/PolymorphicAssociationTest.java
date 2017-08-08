/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.depositor;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.INFINISPAN;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.INFINISPAN_EMBEDDED;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MAP;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MONGODB;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.NEO4J_BOLT;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.NEO4J_HTTP;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "OGM-1294")
@SkipByDatastoreProvider(
		value = { MONGODB, NEO4J_BOLT, NEO4J_HTTP, NEO4J_EMBEDDED, MAP, INFINISPAN_EMBEDDED, INFINISPAN, INFINISPAN_REMOTE },
		comment = "They don't support queries on polymorphic entities using TABLE_PER_CLASS inheritance strategy; requires multiple queries")
public class PolymorphicAssociationTest extends OgmJpaTestCase {

	private Depositor depositor = new Depositor( "Max", "Payne" );

	@Before
	public void setUp() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			depositor.addAddress( new Address( "Unknown", "111111", "Unknown", "Unknown" ) );
			depositor.addContactDetail( new ContactDetail( ContactType.EMAIL, "max@payne.com" ) );
			AccountEntry entry1 = new AccountEntry( 30000.0 );
			AccountEntry entry2 = new AccountEntry( 70000.0 );
			GiroAccount account = new GiroAccount( depositor, 555.5d );
			account.addEntry( entry1 );
			account.addEntry( entry2 );
			depositor.addAccount( account );
			em.persist( depositor );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testPolymorphicAssociation() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			// This em.clear() is important for the test; DON'T REMOVE IT.
			// It makes sure that the giro account we created before is not in the cache and therefore
			// the dialect doesn't know if the value we are looking for is a GiroAccount or not.
			em.clear();

			final TypedQuery<Depositor> query = em.createQuery( "SELECT d FROM Depositor d WHERE d.name='Max'", Depositor.class );
			final Depositor entity = query.getSingleResult();

			assertThat( entity ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "Max" );
			assertThat( entity.getSurname() ).isEqualTo( "Payne" );

			final Set<ContactDetail> contactDetails = entity.getContactDetails();
			assertThat( contactDetails.size() ).isEqualTo( 1 );
			final ContactDetail contactDetail = contactDetails.iterator().next();
			assertThat( contactDetail.getType() ).isEqualTo( ContactType.EMAIL );
			assertThat( contactDetail.getValue() ).isEqualTo( "max@payne.com" );

			final Set<Address> addresses = entity.getAddresses();
			assertThat( addresses.size() ).isEqualTo( 1 );
			final Address address = addresses.iterator().next();
			assertThat( address.getCountry() ).isEqualTo( "Unknown" );
			assertThat( address.getZipCode() ).isEqualTo( "111111" );
			assertThat( address.getCity() ).isEqualTo( "Unknown" );
			assertThat( address.getStreet() ).isEqualTo( "Unknown" );

			final Set<Account> accounts = entity.getAccounts();
			assertThat( accounts.size() ).isEqualTo( 1 );
			final Account account = accounts.iterator().next();
			assertThat( account ).isInstanceOf( GiroAccount.class );

			final GiroAccount giroAccount = (GiroAccount) account;
			assertThat( giroAccount.getBalance() ).as( "balance" ).isEqualTo( 100000.0 );
			assertThat( giroAccount.getCreditLimit() ).as( "credit" ).isEqualTo( 555.5 );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Address.class, Depositor.class, ContactDetail.class, InstantAccessAccount.class, GiroAccount.class, AccountEntry.class };
	}
}
