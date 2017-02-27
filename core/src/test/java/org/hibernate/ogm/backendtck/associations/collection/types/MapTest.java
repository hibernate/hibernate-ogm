/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.hibernate.ogm.utils.TestHelper.getCurrentDialectType;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber.PhoneNumberId;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "hibernate core doesn't supply required primary key metadata for collections"
)
public class MapTest extends OgmTestCase {

	@Test
	public void testMapOfEntity() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Address home = new Address();
		home.setCity( "Paris" );
		Address work = new Address();
		work.setCity( "San Francisco" );
		User user = new User();
		user.getAddresses().put( "home", home );
		user.getAddresses().put( "work", work );

		session.persist( home );
		session.persist( work );
		session.persist( user );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		user = (User) session.get( User.class, user.getId() );
		// TODO do null value
		assertThat( user.getAddresses() ).as( "Map should have 2 elements" ).hasSize( 2 );
		assertThat( user.getAddresses().get( "home" ).getCity() ).as( "home address should be under home" ).isEqualTo(
				home.getCity() );
		session.delete( user );
		session.delete( session.load( Address.class, home.getId() ) );
		session.delete( session.load( Address.class, work.getId() ) );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testSetElementCollectionStorageAndRemoval() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		User user = new User();
		user.getNicknames().add( "idrA" );
		user.getNicknames().add( "day[9]" );
		session.persist( user );

		User user2 = new User();
		user2.getNicknames().add( "idrA" );
		user2.getNicknames().add( "day[9]" );
		session.persist( user2 );
		tx.commit();

		session.clear();

		if ( getCurrentDialectType().isDocumentStore() ) {
			assertThat( getNumberOfAssociations( sessionFactory, AssociationStorageType.IN_ENTITY ) )
					.describedAs( "Element collections should be stored within the entity document" )
					.isEqualTo( 2 );
			assertThat( getNumberOfAssociations( sessionFactory, AssociationStorageType.ASSOCIATION_DOCUMENT ) )
					.describedAs( "Element collections should be stored within the entity document" )
					.isEqualTo( 0 );
		}

		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );
		assertThat( user.getNicknames() ).containsOnly( "idrA", "day[9]" );
		user.getNicknames().remove( "idrA" );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );
		assertThat( user.getNicknames() ).containsOnly( "day[9]" );
		session.delete( user );

		user2 = (User) session.get( User.class, user2.getId() );
		assertThat( user2.getNicknames() ).containsOnly( "idrA", "day[9]" );
		session.delete( user2 );

		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testRemovalOfMapEntry() throws Exception {
		// Create user with two addresses
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Address home = new Address();
		home.setCity( "Paris" );
		Address work = new Address();
		work.setCity( "San Francisco" );
		User user = new User();
		user.getAddresses().put( "home", home );
		user.getAddresses().put( "work", work );

		session.persist( home );
		session.persist( work );
		session.persist( user );
		tx.commit();
		session.clear();

		// Load user and remove one address
		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );
		user.getAddresses().remove( "work" );

		tx.commit();
		session.clear();

		// assert
		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );

		assertThat( user.getAddresses() ).hasSize( 1 );
		assertThat( user.getAddresses().containsKey( "home" ) ).isTrue();
		assertThat( user.getAddresses().get( "home" ).getCity() ).isEqualTo( home.getCity() );

		// clean up
		session.delete( user );
		session.delete( session.load( Address.class, home.getId() ) );
		session.delete( session.load( Address.class, work.getId() ) );

		tx.commit();
		session.close();
		checkCleanCache();
	}

	@Test
	public void testMapOfEntityWithCompositeId() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		PhoneNumber home = new PhoneNumber( new PhoneNumberId( "DE", 123 ), "Home Phone" );
		PhoneNumber work = new PhoneNumber( new PhoneNumberId( "EN", 456 ), "Work Phone" );
		User user = new User();
		user.getPhoneNumbers().put( "home", home );
		user.getPhoneNumbers().put( "work", work );

		session.persist( home );
		session.persist( work );
		session.persist( user );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );
		assertThat( user.getPhoneNumbers().get( "home" ) ).isNotNull();
		assertThat( user.getPhoneNumbers().get( "home" ).getId() ).isEqualTo( new PhoneNumberId( "DE", 123 ) );
		assertThat( user.getPhoneNumbers().get( "work" ) ).isNotNull();
		assertThat( user.getPhoneNumbers().get( "work" ).getId() ).isEqualTo( new PhoneNumberId( "EN", 456 ) );
		assertThat( user.getPhoneNumbers() ).hasSize( 2 );

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( PhoneNumber.class, home.getId() ) );
		session.delete( session.load( PhoneNumber.class, work.getId() ) );

		tx.commit();
		session.close();
	}

	public void testMapOfComponent() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Map<String, Department> departments = new HashMap<>();
		departments.put( "sawing", new Department( "Sawing", 7 ) );
		departments.put( "sale", new Department( "Sale", 2 ) );
		Enterprise timberTradingInc = new Enterprise( "enterprise-1", departments );

		session.persist( timberTradingInc );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// assert
		timberTradingInc = (Enterprise) session.get( Enterprise.class, "enterprise-1" );

		assertThat( timberTradingInc.getDepartments() ).hasSize( 2 );
		assertThat( timberTradingInc.getDepartments() ).includes( entry( "sawing", new Department( "Sawing", 7 ) ) );
		assertThat( timberTradingInc.getDepartments() ).includes( entry( "sale", new Department( "Sale", 2 ) ) );

		// clean up
		session.delete( timberTradingInc );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testMapWithSimpleValueType() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Enterprise timberTradingInc = new Enterprise( "enterprise-1", null );
		timberTradingInc.getRevenueByDepartment().put( "sale", 1000 );
		timberTradingInc.getRevenueByDepartment().put( "sawing", 2000 );
		timberTradingInc.getRevenueByDepartment().put( "planting", 3000 );

		session.persist( timberTradingInc );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// assert
		timberTradingInc = (Enterprise) session.get( Enterprise.class, "enterprise-1" );

		assertThat( timberTradingInc.getRevenueByDepartment() ).includes( entry( "sawing", 2000 ) );
		assertThat( timberTradingInc.getRevenueByDepartment() ).includes( entry( "sale", 1000 ) );
		assertThat( timberTradingInc.getRevenueByDepartment() ).includes( entry( "planting", 3000 ) );
		assertThat( timberTradingInc.getRevenueByDepartment() ).hasSize( 3 );

		// clean up
		session.delete( timberTradingInc );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testMapWithStringKeyButListStyleEnforced() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		PhoneNumber home = new PhoneNumber( new PhoneNumberId( "DE", 123 ), "Home Phone" );
		PhoneNumber work = new PhoneNumber( new PhoneNumberId( "EN", 456 ), "Work Phone" );
		User user = new User();
		user.getAlternativePhoneNumbers().put( "home", home );
		user.getAlternativePhoneNumbers().put( "work", work );

		session.persist( home );
		session.persist( work );
		session.persist( user );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		user = (User) session.get( User.class, user.getId() );
		assertThat( user.getAlternativePhoneNumbers().get( "home" ) ).isNotNull();
		assertThat( user.getAlternativePhoneNumbers().get( "home" ).getId() ).isEqualTo( new PhoneNumberId( "DE", 123 ) );
		assertThat( user.getAlternativePhoneNumbers().get( "work" ) ).isNotNull();
		assertThat( user.getAlternativePhoneNumbers().get( "work" ).getId() ).isEqualTo( new PhoneNumberId( "EN", 456 ) );
		assertThat( user.getAlternativePhoneNumbers() ).hasSize( 2 );

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( PhoneNumber.class, home.getId() ) );
		session.delete( session.load( PhoneNumber.class, work.getId() ) );

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Address.class, PhoneNumber.class, Enterprise.class };
	}
}
