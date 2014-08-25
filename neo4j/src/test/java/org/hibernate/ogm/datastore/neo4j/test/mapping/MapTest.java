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

import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
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
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

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

		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		String userNode = "(u:User:ENTITY { id: {u}.id })";
		String addressNode = "(a:Address:ENTITY {id: {a}.id, city: {a}.city})";
		String nickNameNode = "(n:Nicks:EMBEDDED {nicknames: {n}.nicknames})";

		// Expected nodes
		assertExpectedMapping( "u", userNode, params( user ) );
		assertExpectedMapping( "a", addressNode, params( home ) );
		assertExpectedMapping( "a", addressNode, params( work ) );
		assertExpectedMapping( "n", nickNameNode, params( "idrA" ) );
		assertExpectedMapping( "n", nickNameNode, params( "day[9]" ) );
		assertNumberOfNodes( 5 );

		assertExpectedMapping( "r", userNode + " - [r:nicknames] - " + nickNameNode, params( "idrA" ) );
		assertExpectedMapping( "r", userNode + " - [r:nicknames] - " + nickNameNode, params( "day[9]" ) );
		assertExpectedMapping( "r", userNode + " - [r:addresses{addressType: {r}.addressType}] - " + addressNode, params( home, "home" ) );
		assertExpectedMapping( "r", userNode + " - [r:addresses{addressType: {r}.addressType}] - " + addressNode, params( work, "work" ) );
		assertRelationships( 4 );
	}

	private Map<String, Object> params(Address address, String key) {
		Map<String, Object> relationshipProperties = new HashMap<String, Object>();
		relationshipProperties.put( "addressType", key );

		Map<String, Object> params = params( user );
		params.putAll( params( address ) );
		params.put( "r", relationshipProperties );
		return params;
	}

	private Map<String, Object> params(User user) {
		Map<String, Object> userProperties = new HashMap<String, Object>();
		userProperties.put( "id", user.getId() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "u", userProperties );
		return params;
	}

	private Map<String, Object> params(Address address) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "id", address.getId() );
		properties.put( "city", address.getCity() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "a", properties );
		return params;
	}

	private Map<String, Object> params(String nick) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "nicknames", nick );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "n", properties );
		params.putAll( params( user ) );
		return params;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { User.class, Address.class };
	}

}
