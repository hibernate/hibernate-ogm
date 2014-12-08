/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.test.associations;

import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.Car;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.Tire;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ManyToManyCompositeKeyMongoDBFormatTest extends OgmTestCase {
	@Test
	public void testManyToManyCompositeId() throws Exception {
		OgmSession session = openSession();
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


		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Car",
				// query
				"{ '_id' : { 'maker' : 'Citroen', 'model' : 'AX' } }",
				// expected
				"{ '_id' : { 'maker' : 'Citroen', 'model' : 'AX' }, 'hp' : 20, 'tires' : [ { 'maker' : 'Michelin', 'model' : 'B1' } ] }"
		);
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Tire",
				// query
				"{ '_id' : { 'maker' : 'Michelin', 'model' : 'B1' } }",
				// expected
				"{ '_id' : { 'maker' : 'Michelin', 'model' : 'B1' }, 'size' : 17.0, 'cars' : [ { 'maker' : 'Citroen', 'model' : 'AX' } ] }"
		);

		transaction = session.beginTransaction();
		car = (Car) session.get( Car.class, car.getCarId() );
		assertThat( car.getTires() ).hasSize( 1 );
		assertThat( car.getTires().iterator().next().getCars() ).contains( car );
		session.delete( car );
		session.delete( car.getTires().iterator().next() );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Car.class,
				Tire.class
		};
	}
}
