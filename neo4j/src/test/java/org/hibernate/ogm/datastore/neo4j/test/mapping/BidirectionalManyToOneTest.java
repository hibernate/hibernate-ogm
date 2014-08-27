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

import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class BidirectionalManyToOneTest extends Neo4jJpaTestCase {

	private SalesGuy simon;
	private SalesGuy eric;
	private SalesForce salesForce;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		salesForce = new SalesForce( "red_hat" );
		salesForce.setCorporation( "Red Hat" );
		em.persist( salesForce );

		eric = new SalesGuy( "eric" );
		eric.setName( "Eric" );
		eric.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( eric );
		em.persist( eric );

		simon = new SalesGuy( "simon" );
		simon.setName( "Simon" );
		simon.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( simon );
		em.persist( simon );

		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String forceNode = "(f:SalesForce:ENTITY { id: {f}.id, corporation: {f}.corporation})";
		String guyNode = "(g:SalesGuy:ENTITY {id: {g}.id, name: {g}.name})";
		String relationship = forceNode + " - [r:salesForce] - " + guyNode;

		assertExpectedMapping( "f", forceNode, params( salesForce ) );
		assertExpectedMapping( "g", guyNode, params( eric ) );
		assertExpectedMapping( "g", guyNode, params( simon ) );

		assertExpectedMapping( "r", relationship, params( eric, salesForce ) );
		assertExpectedMapping( "r", relationship, params( simon, salesForce ) );
	}

	private Map<String, Object> params(SalesGuy salesGuy) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "g", properties( salesGuy ) );
		return params;
	}

	private Map<String, Object> params(SalesForce salesForce) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "f", properties( salesForce ) );
		return params;
	}

	private Map<String, Object> params(SalesGuy salesGuy, SalesForce salesForce) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.putAll( params( salesForce ) );
		params.putAll( params( salesGuy ) );
		return params;
	}

	private Map<String, Object> properties(SalesForce salesForce2) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "id", salesForce.getId() );
		properties.put( "corporation", salesForce2.getCorporation() );
		return properties;
	}

	private Map<String, Object> properties(SalesGuy salesGuy) {
		Map<String, Object> salesGuyProperties = new HashMap<String, Object>();
		salesGuyProperties.put( "id", eric.getId() );
		salesGuyProperties.put( "name", eric.getName() );
		return salesGuyProperties;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { SalesForce.class, SalesGuy.class };
	}

}
