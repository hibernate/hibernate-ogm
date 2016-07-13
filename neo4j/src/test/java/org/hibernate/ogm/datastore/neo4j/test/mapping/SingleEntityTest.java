/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.manytoone.JUG;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleEntityTest extends Neo4jJpaTestCase {

	private JUG jug;

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		jug = new JUG( "summer_camp" );
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions jugNode = node( "jug", JUG.class.getSimpleName(), ENTITY.name() )
				.property( "jug_id", jug.getId() )
				.property( "name", jug.getName() );

		assertThatOnlyTheseNodesExist( jugNode );
		assertNumberOfRelationships( 0 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { JUG.class };
	}

}
