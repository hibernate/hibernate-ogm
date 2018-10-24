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
import static org.hibernate.ogm.test.storedprocedures.MockStoredProcedureDialect.EXCEPTIONAL_PROCEDURE_NAME;
import static org.hibernate.ogm.test.storedprocedures.MockStoredProcedureDialect.INVALID_PARAM;
import static org.hibernate.ogm.test.storedprocedures.MockStoredProcedureDialect.NOT_EXISTING_PROCEDURE_NAME;
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
 * <p>
 * {@link INFINISPAN_REMOTE} dialect is skipped in this test, the test is provided and
 * run by the dialect module test, using both java and javascript procedure.
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@SkipByGridDialect(
		value = { HASHMAP, INFINISPAN_REMOTE, NEO4J_REMOTE, MONGODB },
		comment = "These dialects don't support stored procedures with named parameters")
@TestForIssue(jiraKey = { "OGM-359" })
public class NamedParametersStoredProcedureCallTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected EntityManager em;

	@Before
	public void setUp() throws Exception {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testSingleResultDynamicCall() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery( SIMPLE_VALUE_PROC );
			storedProcedureQuery.registerStoredProcedureParameter(
					UNIQUE_VALUE_PROC_PARAM, Integer.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( UNIQUE_VALUE_PROC_PARAM, 1 );

			Number singleResult = (Number) storedProcedureQuery.getSingleResult();
			assertThat( singleResult.intValue() ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testResultSetDynamicCallWithResultClass() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					RESULT_SET_PROC, Car.class );

			storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
			storedProcedureQuery.registerStoredProcedureParameter(
					RESULT_SET_PROC_ID_PARAM, Integer.class, ParameterMode.IN );
			storedProcedureQuery.registerStoredProcedureParameter(
					RESULT_SET_PROC_TITLE_PARAM, String.class, ParameterMode.IN );

			storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 1 );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title" );

			@SuppressWarnings("unchecked")
			List<Car> listResult = storedProcedureQuery.getResultList();
			assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
		} );
	}

	@Test
	public void testResultSetDynamicCallWithResultMapping() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					RESULT_SET_PROC, "carMapping" );

			storedProcedureQuery.registerStoredProcedureParameter( "result", Void.class, ParameterMode.REF_CURSOR );
			storedProcedureQuery.registerStoredProcedureParameter(
					RESULT_SET_PROC_ID_PARAM, Integer.class, ParameterMode.IN );
			storedProcedureQuery.registerStoredProcedureParameter(
					RESULT_SET_PROC_TITLE_PARAM, String.class, ParameterMode.IN );

			storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'1" );

			@SuppressWarnings("unchecked")
			List<Car> listResult = storedProcedureQuery.getResultList();
			assertThat( listResult ).containsExactly( new Car( 2, "title'1" ) );
		} );
	}

	@Test
	public void testResultSetStaticCallWithResultClass() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createNamedStoredProcedureQuery(
					"returnNamedParametersWithEntity" );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 1 );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title" );

			@SuppressWarnings("unchecked")
			List<Car> listResult = storedProcedureQuery.getResultList();
			assertThat( listResult ).containsExactly( new Car( 1, "title" ) );
		} );
	}

	@Test
	public void testResultSetStaticCallWithResultMapping() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createNamedStoredProcedureQuery(
					"returnNamedParametersWithMapping" );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'2" );

			@SuppressWarnings("unchecked")
			List<Car> listResult = storedProcedureQuery.getResultList();
			assertThat( listResult ).containsExactly( new Car( 2, "title'2" ) );
		} );
	}

	@Test
	public void testResultSetStaticCallRaw() throws Exception {
		inTransaction( entityManager -> {
			StoredProcedureQuery storedProcedureQuery = entityManager.createNamedStoredProcedureQuery(
					"returnNamedParametersRaw" );
			// First parameter is void
			storedProcedureQuery.setParameter( RESULT_SET_PROC_ID_PARAM, 2 );
			storedProcedureQuery.setParameter( RESULT_SET_PROC_TITLE_PARAM, "title'2" );

			@SuppressWarnings("unchecked")
			List listResult = storedProcedureQuery.getResultList();
			assertThat( listResult ).hasSize( 2 );
			assertThat( ( (Number) listResult.get( 0 ) ).intValue() ).isEqualTo( 2 );
			assertThat( listResult.get( 1 ) ).isEqualTo( "title'2" );
		} );
	}

	@Test
	public void testExceptionWhenMultipleEntitiesAreUsed() throws Exception {
		inTransaction( entityManager -> {
			thrown.expect( PersistenceException.class );
			thrown.expectCause( isA( HibernateException.class ) );
			thrown.expectMessage(
					"org.hibernate.HibernateException: OGM000090: Returning multiple entities is not supported. Procedure '" + RESULT_SET_PROC
							+ "' expects results of type [Car, Motorbike]" );

			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					RESULT_SET_PROC, Car.class, Motorbike.class );
			storedProcedureQuery.registerStoredProcedureParameter( 0, Void.class, ParameterMode.REF_CURSOR );
			storedProcedureQuery.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
			storedProcedureQuery.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( 1, 1 );
			storedProcedureQuery.setParameter( 2, "title" );
			storedProcedureQuery.getResultList();
		} );
	}

	@Test
	public void testExceptionWhenProcedureDoesNotExist() throws Exception {
		inTransaction( entityManager -> {
			thrown.expect( PersistenceException.class );
			thrown.expectMessage( "org.hibernate.HibernateException: OGM000093" );
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					NOT_EXISTING_PROCEDURE_NAME, Car.class );
			storedProcedureQuery.registerStoredProcedureParameter( "param", Integer.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( "param", 1 );
			storedProcedureQuery.getSingleResult();
		} );
	}

	@Test
	@SkipByGridDialect(
			value = { NEO4J_EMBEDDED, NEO4J_REMOTE },
			comment = "Work fine for Neo4j, because function still accepts integer value as a parameter")
	public void testExceptionWhenUsingNotRegisteredParameter() throws Exception {
		inTransaction( entityManager -> {
			thrown.expect( PersistenceException.class );
			thrown.expectMessage( "org.hibernate.HibernateException: OGM000095" );
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					Car.SIMPLE_VALUE_PROC, Integer.class );
			storedProcedureQuery.registerStoredProcedureParameter( INVALID_PARAM, Integer.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( INVALID_PARAM, 1 );
			storedProcedureQuery.getSingleResult();
		} );
	}

	@Test
	public void testExceptionWhenProcedureFails() throws Exception {
		inTransaction( entityManager -> {
			thrown.expect( PersistenceException.class );
			thrown.expectMessage( "org.hibernate.HibernateException: OGM000092" );
			StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery(
					EXCEPTIONAL_PROCEDURE_NAME, Integer.class );
			storedProcedureQuery.registerStoredProcedureParameter( "param", Integer.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( "param", 1 );
			storedProcedureQuery.getSingleResult();
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class, Motorbike.class };
	}
}
