/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.storedprocedures;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC_ID_PARAM;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC_TITLE_PARAM;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testing call of stored procedures
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
@TestForIssue(jiraKey = { "OGM-1429" })
public class InfinispanNamedParametersStoredProcedureCallTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testResultSetDynamicCallWithFullQualifiedProcedureNameAndResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( "org.hibernate.ogm.datastore.infinispan.test.storedprocedures.ResultSetProcedure", Car.class );
		storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( RESULT_SET_PROC_ID_PARAM, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( RESULT_SET_PROC_TITLE_PARAM, String.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 1 );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title" );
		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
	}

	@Test
	public void testExceptionWhenCallingARunnableProcedure() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM001111" );
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( "invalidStoredProcedure", Void.class );
		storedProcedureQuery.registerStoredProcedureParameter( "param", Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( "param", 1 );
		storedProcedureQuery.getSingleResult();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class };
	}
}
