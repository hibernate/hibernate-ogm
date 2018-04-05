/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC_ID_PARAM;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC_TITLE_PARAM;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.SIMPLE_VALUE_PROC;
import static org.hibernate.ogm.backendtck.storedprocedures.Car.UNIQUE_VALUE_PROC_PARAM;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.HibernateException;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testing call of stored procedures that supports named parameters.
 * <p>
 * Note that the datastore must have defined 2 stored procedures:
 * <p>
 * One that returns a value that can be mapped to {@link Car} and another store procedure that returns the value passed
 * as parameter. The name of the stored procedures are respectively {@link Car#RESULT_SET_PROC} and
 * {@link Car#SIMPLE_VALUE_PROC}.
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@SkipByGridDialect(
		value = { HASHMAP, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE, MONGODB },
		comment = "These dialects don't support stored procedures with named parameters")
@TestForIssue(jiraKey = { "OGM-359" })
public class NamedParametersStoredProcedureCallTest extends OgmJpaTestCase {

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
	public void testSingleResultDynamicCall() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( SIMPLE_VALUE_PROC );
		storedProcedureQuery.registerStoredProcedureParameter( UNIQUE_VALUE_PROC_PARAM, Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( UNIQUE_VALUE_PROC_PARAM, 1 );

		Number singleResult = (Number) storedProcedureQuery.getSingleResult();
		assertThat( singleResult ).isEqualTo( 1 );
	}

	@Test
	public void testResultSetDynamicCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, Car.class );

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
	public void testResultSetDynamicCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, "carMapping" );

		storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( RESULT_SET_PROC_ID_PARAM, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( RESULT_SET_PROC_TITLE_PARAM, String.class, ParameterMode.IN );

		storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'1" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'1" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnNamedParametersWithEntity" );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 1 );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnNamedParametersWithMapping" );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'2" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'2" ) );
	}

	@Test
	public void testResultSetStaticCallRaw() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnNamedParametersRaw" );
		// First parameter is void
		storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
		storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'2" );

		@SuppressWarnings("unchecked")
		List<Object[]> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Object[]{ 2, "title'2" } );
	}

	@Test
	public void testExceptionWhenMultipleEntitiesAreUsed() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectCause( isA( HibernateException.class ) );
		thrown.expectMessage(
				"org.hibernate.HibernateException: OGM000090: Returning multiple entities is not supported. Procedure '" + RESULT_SET_PROC
						+ "' expects results of type [Car, Motorbike]" );

		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, Car.class, Motorbike.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 1, 1 );
		storedProcedureQuery.setParameter( 2, "title" );
		storedProcedureQuery.getResultList();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Car.class, Motorbike.class };
	}
}
