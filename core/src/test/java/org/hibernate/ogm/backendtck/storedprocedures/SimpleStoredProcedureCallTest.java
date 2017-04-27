/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

/**
 * Testing call of stored procedures
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class SimpleStoredProcedureCallTest extends OgmJpaTestCase {

	private EntityManager em;

	@Before
	public void setUp() throws Exception {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();
	}

	@After
	public void tearDown() throws Exception {
		if ( em.getTransaction().getRollbackOnly() ) {
			em.getTransaction().rollback();
		}
		else {
			em.getTransaction().commit();
		}
	}


	@Test
	public void testSimpleCalls() throws Exception {
		//no parameters, no results
		StoredProcedureQuery call1 = em.createStoredProcedureQuery( "testName" );
		assertThat( call1.getParameters() ).hasSize( 0 );
		call1.execute();
		//assertThat( call.getMaxResults() ).isEqualTo( 0 );

		StoredProcedureQuery call2 = em.createStoredProcedureQuery( "testName",Car.class );
		call2.registerStoredProcedureParameter( "carId", Long.class, ParameterMode.IN  );
		call2.registerStoredProcedureParameter( "result", Object.class, ParameterMode.REF_CURSOR  );
		call2.setParameter( "carId",1L );
		call2.execute();
		//List resut = call2.getResultList();

		//assertThat( call2.getParameters() ).hasSize( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class };
	}
}
