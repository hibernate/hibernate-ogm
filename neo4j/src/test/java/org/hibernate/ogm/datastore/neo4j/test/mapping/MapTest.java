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

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class MapTest extends Neo4jJpaTestCase {

	private User user;
	private Address home;
	private Address work;

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		home = new Address();
		home.setCity( "Paris" );

		work = new Address();
		work.setCity( "San Francisco" );

		user = new User();
		user.getAddresses().put( "home", home );
		user.getAddresses().put( "work", work );

		user.getNicknames().add( "idrA" );
		user.getNicknames().add( "day[9]" );

		em.persist( home );
		em.persist( work );
		em.persist( user );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions userNode = node( "user", User.class.getSimpleName(), ENTITY.name() )
				.property( "id", user.getId() );

		NodeForGraphAssertions homeNode = node( "home", Address.class.getSimpleName(), ENTITY.name() )
				.property( "id", home.getId() )
				.property( "city", home.getCity() );

		NodeForGraphAssertions workNode = node( "work", Address.class.getSimpleName(), ENTITY.name() )
				.property( "id", work.getId() )
				.property( "city", work.getCity() );

		NodeForGraphAssertions nickNode1 = node( "nick1", "Nicks", EMBEDDED.name() )
				.property( "nicknames", "idrA" );

		NodeForGraphAssertions nickNode2 = node( "nick2", "Nicks", EMBEDDED.name() )
				.property( "nicknames", "day[9]" );

		assertThatOnlyTheseNodesExist(
				userNode
				, homeNode
				, workNode
				, nickNode1
				, nickNode2
				);

		RelationshipsChainForGraphAssertions relationship1 = userNode.relationshipTo( nickNode1, "nicknames" );
		RelationshipsChainForGraphAssertions relationship2 = userNode.relationshipTo( nickNode2, "nicknames" );
		RelationshipsChainForGraphAssertions relationship3 = userNode.relationshipTo( homeNode, "addresses" ).property( "addressType", "home" );
		RelationshipsChainForGraphAssertions relationship4 = userNode.relationshipTo( workNode, "addresses" ).property( "addressType", "work" );

		assertThatOnlyTheseRelationshipsExist(
				relationship1
				, relationship2
				, relationship3
				, relationship4
				);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Address.class, PhoneNumber.class };
	}

}
