/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.Father;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class UnidirectionalManyToOneWithIndexTest extends Neo4jJpaTestCase {

	private Father father1;
	private Child child11;
	private Child child12;

	private Father father2;
	private Child child21;
	private Child child22;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		child11 = new Child();
		child11.setName( "Emmanuel" );
		em.persist( child11 );

		child12 = new Child();
		child12.setName( "Christophe" );
		em.persist( child12 );

		father1 = new Father();
		father1.getOrderedChildren().add( child11 );
		father1.getOrderedChildren().add( child12 );

		em.persist( father1 );

		child21 = new Child();
		child21.setName( "Caroline" );
		em.persist( child21 );

		child22 = new Child();
		child22.setName( "Thomas" );
		em.persist( child22 );

		father2 = new Father();
		father2.getOrderedChildren().add( child21 );
		father2.getOrderedChildren().add( child22 );

		em.persist( father2 );
		commitOrRollback( true );
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 6 );
		assertRelationships( 4 );

		String fatherNode = "(f:Father:ENTITY {id: {f}.id })";
		String childNode = "(c:Child:ENTITY {id: {c}.id, name: {c}.name })";
		String relationshipCypher = fatherNode + " - [r:orderedChildren {birthorder: {r}.birthorder}] - " + childNode;

		assertExpectedMapping( "r", relationshipCypher, params( father1, child11, 0 ) );
		assertExpectedMapping( "r", relationshipCypher, params( father1, child12, 1 ) );
		assertExpectedMapping( "r", relationshipCypher, params( father2, child21, 0 ) );
		assertExpectedMapping( "r", relationshipCypher, params( father2, child22, 1 ) );
	}

	private Map<String, Object> params(Father father, Child child, int birthOrder) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "f", properties( father ) );
		params.put( "c", properties( child ) );
		params.put( "r", singletonMap( "birthorder", birthOrder ) );
		return params;
	}

	private Map<String, Object> properties(Father father) {
		Map<String, Object> fatherProperties = new HashMap<String, Object>();
		fatherProperties.put( "id", father.getId() );
		return fatherProperties;
	}

	private Map<String, Object> properties(Child child) {
		Map<String, Object> childProperties = new HashMap<String, Object>();
		childProperties.put( "id", child.getId() );
		childProperties.put( "name", child.getName() );
		return childProperties;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Father.class, Child.class };
	}

}
