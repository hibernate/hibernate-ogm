/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;
import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SimpleQueriesWithTablePerClassInheritanceTest extends OgmTestCase {

	private Person joe = new Person( "Joe" );
	private CommunityMember sergey = new CommunityMember( "Sergey", "Hibernate OGM" );
	private Employee davide = new Employee( "Davide", "Hibernate OGM", "Red Hat" );

	@Before
	public void prepareDb() {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.persist( joe );
			session.persist( davide );
			session.persist( sergey );
			session.flush();
			tx.commit();
		}
	}

	@After
	public void deleteData() {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.delete( session.merge( joe ) );
			session.delete( session.merge( davide ) );
			session.delete( session.merge( sergey ) );
			session.flush();
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-732")
	@SkipByGridDialect(value = { MONGODB, NEO4J_EMBEDDED, NEO4J_REMOTE })
	public void testResultsFromPerson() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			List<?> results = session.createQuery( "from Person p" ).list();
			assertThat( results ).onProperty( "name" ).containsOnly( davide.name, sergey.name, joe.name );

			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-732")
	@SkipByGridDialect(value = { MONGODB, NEO4J_EMBEDDED, NEO4J_REMOTE })
	public void testResultsFromCommunityMember() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			List<?> results = session.createQuery( "from CommunityMember c" ).list();
			assertThat( results ).onProperty( "name" ).containsOnly( davide.name, sergey.name );

			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-732")
	public void testResultsFromEmployee() throws Exception {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			List<?> results = session.createQuery( "from Employee e" ).list();
			assertThat( results ).onProperty( "name" ).containsOnly( davide.name );

			tx.commit();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class, CommunityMember.class, Employee.class };
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Indexed
	public static class Person {

		@Id
		public String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Person other = (Person) obj;
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}
	}

	@Entity(name = "CommunityMember")
	@Table(name = "CommunityMember")
	@Indexed
	public static class CommunityMember extends Person {

		@Field(analyze = Analyze.NO, store = Store.YES)
		public String project;

		public CommunityMember() {
		}

		public CommunityMember(String name, String project) {
			super( name );
			this.project = project;
		}

		public String getProject() {
			return project;
		}

		public void setProject(String project) {
			this.project = project;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ( ( project == null ) ? 0 : project.hashCode() );
			return result;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "Employee")
	@Indexed
	public static class Employee extends CommunityMember {

		@Field(analyze = Analyze.NO, store = Store.YES)
		public String employer;

		public Employee() {
		}

		public Employee(String name, String project, String employer) {
			super( name, project );
			this.employer = employer;
		}

		public String getEmployer() {
			return employer;
		}

		public void setEmployer(String employer) {
			this.employer = employer;
		}
	}

}
