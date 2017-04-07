/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@SkipByGridDialect( value = { GridDialectType.IGNITE}, comment = "Not implemented in dialect yet!")
@TestForIssue(jiraKey = "OGM-1221")
public class SingleTablenheritancePersistTest extends OgmJpaTestCase {

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
	public void testPersistEntititesWithoutErrors() {
		Man john = new Man( "John" );
		Woman jane = new Woman( "Jane" );
		Child susan = new Child( "Susan" );
		Child mark = new Child( "Mark" );

		List<Child> children = new ArrayList<Child>( Arrays.asList( susan, mark ) );

		jane.setHusband( john );
		jane.setChildren( children );

		john.setWife( jane );
		john.setChildren( children );

		for ( Child child : children ) {
			child.setFather( john );
			child.setMother( jane );
		}

		persist( john, jane, susan, mark );
	}

	private void persist(Object... entities) {
		em.getTransaction().begin();
		for ( Object entity : entities ) {
			em.persist( entity );
		}
		em.flush();
		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Child.class, Man.class, Person.class, Woman.class };
	}
}
