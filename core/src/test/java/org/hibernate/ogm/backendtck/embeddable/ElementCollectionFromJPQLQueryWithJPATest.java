/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * Tests that {@link ElementCollection} is loaded within the entity when it's the result of a JPQL query using JPA.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1420")
public class ElementCollectionFromJPQLQueryWithJPATest extends OgmJpaTestCase {

	@After
	public void cleanUp() throws Exception {
		removeEntities();
	}

	@Test
	public void testQueryWithoutResultType() {
		QtyContents content1 = new QtyContents( "average1", "quantity1" );
		QtyContents content2 = new QtyContents( "average2", "quantity2" );
		FoodsCosmeticsMedicines medicine = new FoodsCosmeticsMedicines( "medicine" );
		medicine.getQtyContentsList().add( content1 );
		medicine.getQtyContentsList().add( content2 );

		inTransaction( em -> {
			em.persist( medicine );
		} );

		inTransaction( em -> {
			@SuppressWarnings("unchecked")
			List<FoodsCosmeticsMedicines> loaded = em.createQuery( "FROM FoodsCosmeticsMedicines" ).getResultList();

			assertThat( loaded ).containsOnly( medicine );
			assertThat( loaded.get( 0 ).getQtyContentsList() ).containsOnly( content1, content2 );
		} );
	}

	@Test
	public void testQueryWithResultType() {
		QtyContents content1 = new QtyContents( "average1", "quantity1" );
		QtyContents content2 = new QtyContents( "average2", "quantity2" );
		FoodsCosmeticsMedicines medicine = new FoodsCosmeticsMedicines( "medicine" );
		medicine.getQtyContentsList().add( content1 );
		medicine.getQtyContentsList().add( content2 );

		inTransaction( em -> {
			em.persist( medicine );
		} );

		inTransaction( em -> {
			List<FoodsCosmeticsMedicines> loaded = em.createQuery( "FROM FoodsCosmeticsMedicines", FoodsCosmeticsMedicines.class ).getResultList();

			assertThat( loaded ).containsOnly( medicine );
			assertThat( loaded.get( 0 ).getQtyContentsList() ).containsOnly( content1, content2 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ FoodsCosmeticsMedicines.class };
	}
}
