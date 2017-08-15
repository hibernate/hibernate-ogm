/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleTableInheritancePersistTest extends OgmJpaTestCase {

	private Man john = new Man( "John", "Riding Roller Coasters" );
	private Woman jane = new Woman( "Jane", "Hippotherapist" );
	private Child susan = new Child( "Susan", "Super Mario retro Mushroom" );
	private Child mark = new Child( "Mark", "Fidget Spinner" );
	private Family family = new Family( "McCloud" );

	private final List<Person> familyMembers = Arrays.asList( john, jane, susan, mark );
	private final String[] peopleNames = extractNames( familyMembers );

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		try {
			em.getTransaction().begin();
			Family found = em.find( Family.class, family.getName() );
			if ( found != null ) {
				em.remove( found );
			}
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1221")
	public void testPersistEntititesWithoutErrors() {
		initDB();
	}

	@Test
	public void testCascadeOperation() {
		initDB();

		em.getTransaction().begin();
		em.remove( em.find( Family.class, family.getName() ) );
		em.getTransaction().commit();

		em.clear();
		em.getTransaction().begin();
		List<Person> people = em.createQuery( "FROM Person p", Person.class ).getResultList();
		assertThat( people ).isEmpty();
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testJpqlWithSingleResult() {
		initDB();
		em.getTransaction().begin();
		Person person = em.createQuery( "FROM Person p WHERE p.name = 'Jane'", Person.class ).getSingleResult();
		assertThat( person.getName() ).isEqualTo( jane.getName() );
		assertThat( person ).isInstanceOf( Woman.class );
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testJpqlReturnPropertiesFromSuperClass() {
		initDB();
		em.getTransaction().begin();
		List<Person> persons = em.createQuery( "FROM Person p", Person.class ).getResultList();
		String[] actualNames = extractNames( persons );
		assertThat( actualNames ).containsOnly( peopleNames );
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testJpqlReturnPropertiesForMan() {
		initDB();
		em.getTransaction().begin();
		List<Person> persons = em.createQuery( "FROM Person p where p.name = :name", Person.class ).setParameter( "name", john.getName() ).getResultList();
		assertThat( persons ).hasSize( 1 );
		assertThat( persons.get( 0 ) ).isInstanceOf( Man.class );
		Man man = (Man) ( persons.get( 0 ) );
		assertThat( man.getHobby() ).isEqualTo( john.getHobby() );
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testJpqlReturnPropertiesForChildren() {
		initDB();
		em.getTransaction().begin();
		List<Person> persons = em.createQuery( "FROM Person p where p.name IN (:names)", Person.class )
				.setParameter( "names", Arrays.asList( susan.getName(), mark.getName() ) ).getResultList();

		assertThat( persons ).hasSize( 2 );

		for ( Person person : persons ) {
			assertThat( person ).isInstanceOf( Child.class );
			assertThat( ( (Child) person ).getFavouriteToy() ).isIn( susan.getFavouriteToy(), mark.getFavouriteToy() );
		}
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testJpqlReturnPropertiesForSubClasses() {
		initDB();
		em.getTransaction().begin();
		List<Person> persons = em.createQuery( "FROM Person p", Person.class ).getResultList();
		for ( Person person : persons ) {
			if ( person instanceof Man ) {
				assertThat( ( (Man) person ).getHobby() ).isEqualTo( john.getHobby() );
			}
			else if ( person instanceof Woman ) {
				assertThat( ( (Woman) person ).getJob() ).isEqualTo( jane.getJob() );
			}
			else if ( person instanceof Child ) {
				assertThat( ( (Child) person ).getFavouriteToy() ).isIn( susan.getFavouriteToy(), mark.getFavouriteToy() );
			}
			else {
				fail( "Unexpected result: " + person );
			}
		}
		em.getTransaction().commit();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1294")
	public void testPolymorphicAssociation() {
		initDB();
		em.getTransaction().begin();
		Family family = em.createQuery( "FROM Family f", Family.class ).getSingleResult();
		List<Person> members = family.getMembers();
		assertThat( members ).hasSize( familyMembers.size() );
		for ( Person person : members ) {
			if ( person instanceof Man ) {
				assertThat( ( (Man) person ).getHobby() ).isEqualTo( john.getHobby() );
			}
			else if ( person instanceof Woman ) {
				assertThat( ( (Woman) person ).getJob() ).isEqualTo( jane.getJob() );
			}
			else if ( person instanceof Child ) {
				assertThat( ( (Child) person ).getFavouriteToy() ).isIn( susan.getFavouriteToy(), mark.getFavouriteToy() );
			}
			else {
				fail( "Unexpected result: " + person );
			}
		}
		em.getTransaction().commit();
	}

	private String[] extractNames(List<Person> persons) {
		List<String> names = new ArrayList<>();
		for ( Person person : persons ) {
			names.add( person.getName() );
		}
		return names.toArray( new String[names.size()] );
	}

	private void initDB() {
		List<Child> children = new ArrayList<Child>( Arrays.asList( susan, mark ) );

		jane.setHusband( john );
		jane.setChildren( children );

		john.setWife( jane );
		john.setChildren( children );

		for ( Child child : children ) {
			child.setFather( john );
			child.setMother( jane );
		}

		for ( Person person : familyMembers ) {
			family.add( person );
		}

		em.getTransaction().begin();
		em.persist( family );
		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Child.class, Man.class, Person.class, Woman.class, Family.class };
	}
}
