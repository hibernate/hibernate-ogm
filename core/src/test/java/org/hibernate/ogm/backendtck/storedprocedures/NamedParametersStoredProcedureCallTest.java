/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_RESULT_SET_STORED_PROC;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_SIMPLE_VALUE_STORED_PROC;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing call of stored procedures that supports named parameters.
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@SkipByGridDialect(
		value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE, MONGODB },
		comment = "These dialects don't support stored procedures with named parameters")
@TestForIssue(jiraKey = { "OGM-359" })
public class NamedParametersStoredProcedureCallTest extends OgmJpaTestCase {

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
	public void testSingleResultDynamicCall() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( TEST_SIMPLE_VALUE_STORED_PROC );
		storedProcedureQuery.registerStoredProcedureParameter( TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME, Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME, 1 );

		Number singleResult = (Number) storedProcedureQuery.getSingleResult();
		assertThat( singleResult ).isEqualTo( 1 );
	}

	@Test
	public void testResultSetDynamicCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( TEST_RESULT_SET_STORED_PROC, Car.class );

		storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, String.class, ParameterMode.IN );

		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, 1 );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, "title" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
	}

	@Test
	public void testResultSetDynamicCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( TEST_RESULT_SET_STORED_PROC, "carMapping" );

		storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, String.class, ParameterMode.IN );

		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, 2 );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, "title'1" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'1" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "testproc4_3" );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, 1 );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, "title" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "testproc4_4" );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, 2 );
		storedProcedureQuery.setParameter( TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, "title'2" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'2" ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Car.class };
	}
}
