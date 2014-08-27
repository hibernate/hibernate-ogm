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

import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class BidirectionalOneToOneTest extends Neo4jJpaTestCase {

	private Husband husband;
	private Wife wife;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		husband = new Husband( "frederic" );
		husband.setName( "Frederic Joliot-Curie" );

		wife = new Wife( "wife" );
		wife.setName( "Irene Joliot-Curie" );
		wife.setHusband( husband );
		husband.setWife( wife );
		em.persist( husband );
		em.persist( wife );

		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 2 );
		assertRelationships( 1 );

		String wifeNode = "(w:Wife:ENTITY { id: {w}.id, name: {w}.name })";
		String husbandNode = "(h:Husband:ENTITY {id: {h}.id, name: {h}.name})";

		Map<String, Object> wifeProperties = new HashMap<String, Object>();
		wifeProperties.put( "id", wife.getId() );
		wifeProperties.put( "name", wife.getName() );

		Map<String, Object> husbandProperties = new HashMap<String, Object>();
		husbandProperties.put( "id", husband.getId() );
		husbandProperties.put( "name", husband.getName() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "w", wifeProperties );
		params.put( "h", husbandProperties );

		assertExpectedMapping( "w", wifeNode, params );
		assertExpectedMapping( "h", husbandNode, params );
		assertExpectedMapping( "r", wifeNode + " - [r:wife] - " + husbandNode, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Husband.class, Wife.class };
	}

}
