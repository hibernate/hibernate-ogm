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

import org.hibernate.ogm.backendtck.associations.manytoone.JUG;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleEntityTest extends Neo4jJpaTestCase {

	private JUG jug;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		jug = new JUG( "summer_camp" );
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 1 );
		assertRelationships( 0 );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "jug_id", jug.getId() );
		properties.put( "name", jug.getName() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "n", properties );

		assertExpectedMapping( "n", "(n:JUG:ENTITY {jug_id: {n}.jug_id, name: {n}.name})", params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { JUG.class };
	}

}
