/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static javax.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Test that @Inheritance(strategy = TABLE_PER_CLASS) with generated id work correct
 *
 */
public class TableIdGeneratorInheritanceTest extends OgmJpaTestCase {

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testTableIdGenerator() {
		em.getTransaction().begin();
		Truck truck = new Truck();
		em.persist( truck );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();

		Truck loadedTruck = em.find( Truck.class, truck.getId() );

		assertThat( loadedTruck ).isNotNull().as( "truck not loaded" );
		assertThat( loadedTruck.getId() ).isNotNull().as( "truck id not loaded" );
		assertThat( loadedTruck.getId() ).isEqualTo(truck.getId()).as( "loaded truck id is not equals saved truck id" );
		em.getTransaction().commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BaseCar.class,
				Truck.class,
				PassengerCar.class
		};
	}

	@Entity
	@Table(name = "TRUCK")
	private static class Truck extends BaseCar {
	}

	@Entity
	@Table(name = "PASSENGER_CAR")
	private static class PassengerCar extends BaseCar {
	}

	@Entity
	@Table(name = "BASE_CAR")
	@Inheritance(strategy = TABLE_PER_CLASS)
	private abstract static class BaseCar {
		protected UUID id;

		@Id
		@GeneratedValue(generator = "uuid")
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}
	}

}
