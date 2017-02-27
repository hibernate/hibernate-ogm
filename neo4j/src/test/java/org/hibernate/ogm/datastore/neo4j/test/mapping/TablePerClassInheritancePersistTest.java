/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class TablePerClassInheritancePersistTest extends Neo4jJpaTestCase {

	private Man john;
	private Woman jane;
	private Child susan;
	private Child mark;

	@Before
	public void prepareDB() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		john = new Man();
		jane = new Woman();
		susan = new Child();
		mark = new Child();

		john.name = "John";
		john.wife = jane;
		john.children.add( mark );
		john.children.add( susan );

		jane.name = "Jane";
		jane.husband = john;
		jane.children.add( mark );
		jane.children.add( susan );

		susan.name = "Susan";
		susan.mother = jane;
		susan.father = john;

		mark.name = "Mark";
		mark.mother = jane;
		mark.father = john;

		persist( em, john, jane, mark, susan );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions johnNode = node( "john", Man.TABLE_NAME, ENTITY.name() )
				.property( "name", john.name );

		NodeForGraphAssertions janeNode = node( "jane", Woman.TABLE_NAME, ENTITY.name() )
				.property( "name", jane.name );

		NodeForGraphAssertions susanNode = node( "susan", Child.TABLE_NAME, ENTITY.name() )
				.property( "name", susan.name );

		NodeForGraphAssertions markNode = node( "mark", Child.TABLE_NAME, ENTITY.name() )
				.property( "name", mark.name );

		RelationshipsChainForGraphAssertions johnWife = johnNode.relationshipTo( janeNode, "wife" );

		RelationshipsChainForGraphAssertions susanMother = susanNode.relationshipTo( janeNode, "mother" );
		RelationshipsChainForGraphAssertions susanFather = susanNode.relationshipTo( johnNode, "father" );

		RelationshipsChainForGraphAssertions markMother = markNode.relationshipTo( janeNode, "mother" );
		RelationshipsChainForGraphAssertions markFather = markNode.relationshipTo( johnNode, "father" );

		assertThatOnlyTheseNodesExist( johnNode, janeNode, markNode, susanNode );
		assertThatOnlyTheseRelationshipsExist( susanMother, susanFather, markMother, markFather, johnWife );

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class, Man.class, Woman.class, Child.class };
	}

	@Entity
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
	@Table( name = Person.TABLE_NAME )
	class Person {

		public static final String TABLE_NAME = "Person";

		@Id
		String name;
	}

	@Entity
	@DiscriminatorValue("MAN")
	@Table( name = Man.TABLE_NAME )
	class Man extends Person {

		public static final String TABLE_NAME = "Man";

		@OneToOne
		Woman wife;

		@OneToMany(mappedBy = "father")
		List<Child> children = new ArrayList<>();
	}

	@Entity
	@DiscriminatorValue("WOMAN")
	@Table( name = Woman.TABLE_NAME )
	class Woman extends Person {

		public static final String TABLE_NAME = "Woman";

		@OneToOne(mappedBy = "wife")
		Man husband;

		@OneToMany(mappedBy = "mother")
		List<Child> children = new ArrayList<>();
	}

	@Entity
	@DiscriminatorValue("CHILD")
	@Table( name = Child.TABLE_NAME )
	class Child extends Person {

		public static final String TABLE_NAME = "Child";

		@OneToOne
		Woman mother;

		@OneToOne
		Man father;
	}
}
