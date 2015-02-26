/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.innertypes;

import static org.fest.assertions.Assertions.assertThat;
import javax.persistence.EntityManager;
import org.hibernate.ogm.backendtck.innertypes.CommunityMember.Employee;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Verifies the dialect is able to store an entity which is represented as an inner type in Java.
 *
 * For example on MongoDB this implies the name of the class needs to avoid the dollar symbol,
 * so this needs to be adjusted by forcing a valid name using the JPA Table annotation.
 *
 * See also org.hibernate.ogm.datastore.mongodb.test.datastore.CollectionNamingValidationTest in the MongoDB module.
 *
 * @author Davide D'Alto
 * @author Sanne Grinovero
 */
@TestForIssue(jiraKey = "OGM-265")
public class InnerClassFindTest extends JpaTestCase {

	@Test
	public void testInnerClassFind() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		CommunityMember member = new CommunityMember( "Davide" );
		em.persist( member );

		Employee employee = new Employee( "Alex", "EMPLOYER" );
		em.persist( employee );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		CommunityMember lh = em.find( CommunityMember.class, member.name );
		assertThat( lh ).isNotNull();
		assertThat( lh ).isInstanceOf( CommunityMember.class );

		CommunityMember lsh = em.find( Employee.class, employee.name );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf( Employee.class );
		assertThat( ((Employee) employee).employer ).isEqualTo( employee.employer );

		em.remove( lh );
		em.remove( lsh );
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[]{ CommunityMember.class, Employee.class };
	}

}
