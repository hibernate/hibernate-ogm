/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.gen;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.util.List;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.Test;

/**
 * Test user defined {@link IdentifierGenerator}
 */
public class CustomIdGeneratorTest extends OgmJpaTestCase {

	private CustomIdGeneratorEntity customIdGeneratorEntityNo1 = new CustomIdGeneratorEntity( CustomIdGenerator.ID_PREFIX + 1 );
	private CustomIdGeneratorEntity customIdGeneratorEntityNo2 = new CustomIdGeneratorEntity( CustomIdGenerator.ID_PREFIX + 2 );
	private CustomIdGeneratorEntity customIdGeneratorEntityNo3 = new CustomIdGeneratorEntity( CustomIdGenerator.ID_PREFIX + 3 );

	@Test
	public void testIdsFromCustomGenerator() {
		// persisting one entity
		inTransaction( em -> {
			em.persist( new CustomIdGeneratorEntity() );
		} );
		inTransaction( em -> {
			List<CustomIdGeneratorEntity> resultList = em.createQuery( "from CustomIdGeneratorEntity" ).getResultList();
			assertThat( resultList ).containsExactly( customIdGeneratorEntityNo1 );
		} );

		// persisting more entities
		inTransaction( em -> {
			em.persist( new CustomIdGeneratorEntity() );
			em.persist( new CustomIdGeneratorEntity() );
		} );
		inTransaction( em -> {
			List<CustomIdGeneratorEntity> resultList = em.createQuery( "from CustomIdGeneratorEntity" ).getResultList();
			assertThat( resultList ).containsOnly( customIdGeneratorEntityNo1, customIdGeneratorEntityNo2, customIdGeneratorEntityNo3 );
		} );
	}

	@Test
	public void after() throws Exception {
		removeEntities();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CustomIdGeneratorEntity.class };
	}
}
