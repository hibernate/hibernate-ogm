/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.util.UUID;
import java.util.function.Consumer;
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
 */
public class TableIdGeneratorInheritanceTest extends OgmJpaTestCase {

	private static final String someTruckName = "someTruckName";
	private static final String someOtherTruckName = "someOtherTruckName";
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
	public void testTableIdGeneratesAndEqualsSaved() {
		Truck truck = new Truck();
		doInTransaction( em -> em.persist( truck ) );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getId() ).isEqualTo( truck.getId() );
		} );
	}

	@Test
	public void testInheritedTableIdGenerates() {
		Truck truck = new Truck();
		doInTransaction( em -> em.persist( truck ) );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getId() ).isNotNull();
		} );
	}


	@Test
	public void testInheritedTableUpdates() {
		Truck truck = new Truck();
		truck.setName( someTruckName );

		doInTransaction( em -> em.persist( truck ) );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getName() ).isEqualTo( someTruckName );

			loadedTruck.setName( someOtherTruckName );
			em.persist( loadedTruck );

		} );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getName() ).isEqualTo( someOtherTruckName );
		} );
	}

	private void doInTransaction(Consumer<EntityManager> action) {
		em.getTransaction().begin();

		action.accept( em );

		em.getTransaction().commit();
		em.clear();
	}

	@Test
	public void testInheritedTableDelete() {
		Truck truck = new Truck();
		doInTransaction( em -> em.persist( truck ) );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			em.remove( loadedTruck );
		} );

		doInTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck ).isNull();
		} );

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BaseCar.class,
				Truck.class
		};
	}

	@Entity
	@Table(name = "TRUCK")
	private static class Truck extends BaseCar {
	}

	@Entity
	@Table(name = "BASE_CAR")
	@Inheritance(strategy = TABLE_PER_CLASS)
	private abstract static class BaseCar {
		protected UUID id;
		private String name;

		@Id
		@GeneratedValue(generator = "uuid")
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
