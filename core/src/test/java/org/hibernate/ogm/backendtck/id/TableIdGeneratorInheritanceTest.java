/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static javax.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.fest.assertions.Assertions.assertThat;

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

/**
 * Test that @Inheritance(strategy = TABLE_PER_CLASS) with generated id work correct
 */
public class TableIdGeneratorInheritanceTest extends OgmJpaTestCase {

	private static final String PIAGGIO_NAME = "Piaggio Porter";
	private static final String MAZDA_NAME = "Mazda Scrum Wagon";

	private EntityManager em;
	private Truck truck;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		try {
			em.close();
		}
		finally {
			delete( truck );
			truck = null;
		}
	}

	@Test
	public void testIdGeneration() {
		truck = new Truck();
		inTransaction( em -> em.persist( truck ) );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getId() ).isEqualTo( truck.getId() );
		} );

	}

	@Test
	public void testInheritedTableIdGenerates() {
		truck = new Truck();
		inTransaction( em -> em.persist( truck ) );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getId() ).isNotNull();
		} );
	}

	@Test
	public void testInheritedTableUpdates() {
		truck = new Truck( PIAGGIO_NAME );

		inTransaction( em -> em.persist( truck ) );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getName() ).isEqualTo( PIAGGIO_NAME );

			loadedTruck.setName( MAZDA_NAME );
			em.persist( loadedTruck );

		} );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck.getName() ).isEqualTo( MAZDA_NAME );
		} );
	}

	@Test
	public void testInheritedTableDelete() {
		truck = new Truck();
		inTransaction( em -> em.persist( truck ) );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			em.remove( loadedTruck );
		} );

		inTransaction( em -> {
			Truck loadedTruck = em.find( Truck.class, truck.getId() );
			assertThat( loadedTruck ).isNull();
		} );
	}

	private void delete(Truck truck) {
		inTransaction( em -> {
			Truck found = em.find( Truck.class, truck.getId() );
			if ( found != null ) {
				em.remove( found );
			}
		} );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Vehicle.class,
				Truck.class
		};
	}

	@Entity
	@Table(name = "TRUCK")
	private static class Truck extends Vehicle {

		public Truck() {
		}

		public Truck(String name) {
			super( name );
		}
	}

	@Entity
	@Table(name = "VEHICLE")
	@Inheritance(strategy = TABLE_PER_CLASS)
	private abstract static class Vehicle {

		protected UUID id;
		private String name;

		public Vehicle() {
		}

		public Vehicle(String name) {
			this.name = name;
		}

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
