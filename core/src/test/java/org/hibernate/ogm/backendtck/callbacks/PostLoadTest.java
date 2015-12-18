/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * @author David Williams
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "Zoo.animals set - bag semantics unsupported (no primary key)"
)
public class PostLoadTest extends OgmJpaTestCase {

	/**
	 * Load an entity with an embedded collection which uses a @PostLoad annotated method
	 * to record the collection size
	 */
	@Test
	public void testFieldSetInPostLoad() {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Zoo zoo = new Zoo();
		zoo.setId( 1 );

		Set<Animal> animals = new HashSet<Animal>();
		animals.add( createAnimal( "Giraffe" ) );
		animals.add( createAnimal( "Elephant" ) );
		animals.add( createAnimal( "Panda" ) );
		zoo.setAnimals( animals );

		em.persist( zoo );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		zoo = em.find( Zoo.class, zoo.getId() );

		assertNotNull( zoo );
		assertEquals( 3, zoo.getNrOfAnimals() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testFieldSetInPostLoadByListener() {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Zoo zoo = new Zoo();
		zoo.setId( 1 );

		Set<Animal> animals = new HashSet<Animal>();
		animals.add( createAnimal( "Giraffe" ) );
		animals.add( createAnimal( "Elephant" ) );
		animals.add( createAnimal( "Panda" ) );
		zoo.setAnimals( animals );

		em.persist( zoo );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		zoo = em.find( Zoo.class, zoo.getId() );

		assertNotNull( zoo );
		assertEquals( 3, zoo.getNrOfAnimalsByListener() );

		em.getTransaction().commit();
		em.close();
	}

	private Animal createAnimal(String name) {
		Animal animal = new Animal();
		animal.setName( name );

		return animal;
	}

	@After
	public void removeEntities() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Zoo.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Zoo.class };
	}
}
