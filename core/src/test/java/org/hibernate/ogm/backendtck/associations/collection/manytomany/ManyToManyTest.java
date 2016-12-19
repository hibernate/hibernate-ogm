/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;
import static org.hibernate.ogm.utils.TestHelper.get;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;

import java.util.EnumSet;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ManyToManyTest extends OgmTestCase {

	@Test
	public void testBidirectionalManyToMany() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		AccountOwner owner = new AccountOwner( "owner_1" );
		owner.setSSN( "0123456" );
		BankAccount soge = new BankAccount( "account_1" );
		soge.setAccountNumber( "X2345000" );
		owner.getBankAccounts().add( soge );
		soge.getOwners().add( owner );
		session.persist( owner );
		tx.commit();

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 2 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( expectedAssociationNumber() );
		session.clear();

		// read from inverse side
		tx = session.beginTransaction();
		soge = get( session, BankAccount.class, soge.getId() );
		assertThat( soge.getOwners() ).hasSize( 1 );
		assertThat( soge.getOwners() ).onProperty( "id" ).contains( owner.getId() );
		tx.commit();

		session.clear();

		// read from non-inverse side and update data
		tx = session.beginTransaction();
		owner = get( session, AccountOwner.class, owner.getId() );
		assertThat( owner.getBankAccounts() ).hasSize( 1 );
		assertThat( owner.getBankAccounts() ).onProperty( "id" ).contains( soge.getId() );
		BankAccount barclays = new BankAccount( "account_2" );
		barclays.setAccountNumber( "ZZZ-009" );
		barclays.getOwners().add( owner );
		soge = owner.getBankAccounts().iterator().next();
		soge.getOwners().remove( owner );
		owner.getBankAccounts().add( barclays );
		owner.getBankAccounts().remove( soge );
		session.delete( soge );
		tx.commit();

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 2 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( expectedAssociationNumber() );
		session.clear();

		// delete data
		tx = session.beginTransaction();
		owner = get( session, AccountOwner.class, owner.getId() );
		assertThat( owner.getBankAccounts() ).hasSize( 1 );
		assertThat( owner.getBankAccounts() ).onProperty( "id" ).contains( barclays.getId() );
		barclays = owner.getBankAccounts().iterator().next();
		barclays.getOwners().clear();
		owner.getBankAccounts().clear();
		session.delete( barclays );
		session.delete( owner );
		tx.commit();

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 0 );

		session.close();
		checkCleanCache();
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA },
			comment = "composite PKs in associations not yet supported"
	)
	public void testManyToManyCompositeId() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Car car = new Car();
		car.setCarId( new Car.CarId( "Citroen", "AX" ) );
		car.setHp( 20 );
		session.persist( car );
		Tire tire = new Tire();
		tire.setTireId( new Tire.TireId( "Michelin", "B1" ) );
		tire.setSize( 17d );
		car.getTires().add( tire );
		tire.getCars().add( car );
		session.persist( tire );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		car = (Car) session.get( Car.class, car.getCarId() );
		assertThat( car.getTires() ).hasSize( 1 );
		assertThat( car.getTires().iterator().next().getCars() ).contains( car );
		session.delete( car );
		session.delete( car.getTires().iterator().next() );
		transaction.commit();
		session.close();
	}

	private int expectedAssociationNumber() {
		if ( EnumSet.of( NEO4J_EMBEDDED, NEO4J_REMOTE ).contains( TestHelper.getCurrentDialectType() ) ) {
			// In Neo4j relationships are bidirectional
			return 1;
		}
		else {
			return 2;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				AccountOwner.class,
				BankAccount.class,
				Car.class,
				Tire.class,
		};
	}
}
