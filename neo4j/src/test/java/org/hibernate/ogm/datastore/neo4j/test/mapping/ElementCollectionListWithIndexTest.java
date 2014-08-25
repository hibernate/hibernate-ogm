/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class ElementCollectionListWithIndexTest extends Neo4jJpaTestCase {

	private GrandMother granny;
	private GrandChild luke;
	private GrandChild leia;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		luke = new GrandChild();
		luke.setName( "Luke" );

		leia = new GrandChild();
		leia.setName( "Leia" );

		granny = new GrandMother();
		granny.getGrandChildren().add( luke );
		granny.getGrandChildren().add( leia );

		em.persist( granny );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String grannyNode = "(granny:GrandMother:ENTITY { id: {granny}.id })";
		String grandChileNode = "(gc:GrandMother_grandChildren:EMBEDDED {name: {gc}.name})";
		String relationship = grannyNode + " - [r:grandChildren{birthorder:{r}.birthorder}] - " + grandChileNode;

		assertExpectedMapping( "granny", grannyNode, params( null, null ) );
		assertExpectedMapping( "gc", grandChileNode, params( luke, 0 ) );
		assertExpectedMapping( "gc", grandChileNode, params( leia, 1 ) );

		assertExpectedMapping( "r", relationship, params( luke, 0 ) );
		assertExpectedMapping( "r", relationship, params( leia, 1 ) );
	}

	private Map<String, Object> params(GrandChild grandChild, Integer index) {

		Map<String, Object> grannyProperties = new HashMap<String, Object>();
		grannyProperties.put( "id", granny.getId() );

		Map<String, Object> relationshipProperties = new HashMap<String, Object>();
		relationshipProperties.put( "birthorder", index );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "granny", grannyProperties );
		params.put( "r", relationshipProperties );

		if ( grandChild != null ) {
			Map<String, Object> grandChildProperties = new HashMap<String, Object>();
			grandChildProperties.put( "name", grandChild.getName() );

			params.put( "gc", grandChildProperties );
		}
		return params;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { GrandMother.class, Child.class };
	}
}
