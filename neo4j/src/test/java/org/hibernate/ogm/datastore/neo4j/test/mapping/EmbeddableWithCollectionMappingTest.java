/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.Arrays;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.embeddable.Order;
import org.hibernate.ogm.backendtck.embeddable.PhoneNumber;
import org.hibernate.ogm.backendtck.embeddable.ShippingAddress;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class EmbeddableWithCollectionMappingTest extends Neo4jJpaTestCase {

	@Before
	public void prepareDB() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Order order = new Order(
				"order-1",
				"Telescope",
				new ShippingAddress(
						new PhoneNumber(  "+1-222-555-0111", Arrays.asList( "+1-222-555-0222", "+1-202-555-0333" ) ),
						"Planet road 68"
				)
		);

		em.persist( order );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions orderNode = node( "order", Order.class.getSimpleName(), ENTITY.name() )
				.property( "id", "order-1" )
				.property( "item", "Telescope" );

		NodeForGraphAssertions shippingAddressNode = node( "adress", EMBEDDED.name() )
				.property( "addressLine", "Planet road 68" );

		NodeForGraphAssertions phoneNode = node( "phone", EMBEDDED.name() )
				.property( "main", "+1-222-555-0111" );

		NodeForGraphAssertions altenative1 = node( "alt1", EMBEDDED.name() )
				.property( "alternatives", "+1-222-555-0222" );

		NodeForGraphAssertions altenative2 = node( "alt2", EMBEDDED.name() )
				.property( "alternatives", "+1-202-555-0333" );

		RelationshipsChainForGraphAssertions rel1 = orderNode.relationshipTo( shippingAddressNode, "shippingAddress" ).relationshipTo( phoneNode, "phone" ).relationshipTo( altenative1, "alternatives" );
		RelationshipsChainForGraphAssertions rel2 = phoneNode.relationshipTo( altenative2, "alternatives" );

		assertThatOnlyTheseNodesExist(
				orderNode
				, shippingAddressNode
				, phoneNode
				, altenative1
				, altenative2 );

		assertThatOnlyTheseRelationshipsExist( rel1, rel2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Order.class };
	}

}
