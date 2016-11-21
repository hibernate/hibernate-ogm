/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleTableInheritanceTest extends Neo4jJpaTestCase {

	private Person joe = new Person( "Joe" );
	private CommunityMember sergey = new CommunityMember( "Sergey", "Hibernate OGM" );
	private Employee davide = new Employee( "Davide", "Hibernate OGM", "Red Hat" );

	@Before
	public void setUp() {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( joe );
		em.persist( davide );
		em.persist( sergey );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions davideNode = node( "d", Person.class.getSimpleName(), "EMP", ENTITY.name() )
				.property( "DTYPE", "EMP" )
				.property( "name", davide.name )
				.property( "project", davide.project )
				.property( "employer", davide.employer );

		NodeForGraphAssertions sergeyNode = node( "s", Person.class.getSimpleName(), "CMM", ENTITY.name() )
				.property( "DTYPE", "CMM" )
				.property( "project", sergey.project )
				.property( "name", sergey.name );

		NodeForGraphAssertions joeNode = node( "j", Person.class.getSimpleName(), "PRS", ENTITY.name() )
				.property( "DTYPE", "PRS" )
				.property( "name", joe.name );

		assertThatOnlyTheseNodesExist( joeNode, davideNode, sergeyNode );
		assertNumberOfRelationships( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class, CommunityMember.class, Employee.class };
	}

	@Entity
	@Table(name = "Person")
	@DiscriminatorValue(value = "PRS")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	class Person {

		@Id
		public String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "CommunityMember")
	@DiscriminatorValue(value = "CMM")
	class CommunityMember extends Person {

		public String project;

		public CommunityMember() {
		}

		public CommunityMember(String name, String project) {
			super( name );
			this.project = project;
		}
	}

	@Entity
	@Table(name = "Employee")
	@DiscriminatorValue(value = "EMP")
	class Employee extends CommunityMember {

		public String employer;

		public Employee() {
		}

		public Employee(String name, String project, String employer) {
			super( name, project );
			this.employer = employer;
		}
	}

}
