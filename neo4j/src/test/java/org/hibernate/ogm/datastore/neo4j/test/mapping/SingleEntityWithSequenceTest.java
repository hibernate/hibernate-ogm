/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.DistributedRevisionControl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleEntityWithSequenceTest extends Neo4jJpaTestCase {

	private DistributedRevisionControl git;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		git = new DistributedRevisionControl();
		git.setName( "GIT" );
		em.persist( git );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 2 );
		assertRelationships( 0 );
		assertExpectedMapping( "(:DistributedRevisionControl:ENTITY {id: " + git.getId() + ", name: '" + git.getName() + "' })" );
		assertExpectedMapping( "(:hibernate_sequences:SEQUENCE { sequence_name: 'DistributedRevisionControl', current_value: 2 })" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { DistributedRevisionControl.class };
	}

}
