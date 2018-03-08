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
import static org.hibernate.ogm.backendtck.storedprocedures.Car.SIMPLE_VALUE_PROC;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
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
 * Testing call of stored procedures that supports indexed parameters
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
		value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE },
		comment = "These dialects not support stored procedures with positional parameters")
@TestForIssue(jiraKey = { "OGM-359" })
public class PositionalParametersStoredProcedureCallTest extends OgmJpaTestCase {

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
		storedProcedureQuery.registerStoredProcedureParameter( 0, Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 0, 1 );

		Number singleResult = (Number) storedProcedureQuery.getSingleResult();
		assertThat( singleResult.intValue() ).isEqualTo( 1 );
	}

	@Test
	public void testResultSetDynamicCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, Car.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 1, 1 );
		storedProcedureQuery.setParameter( 2, "title" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsOnly( new Car( 1, "title" ) );
	}

	@Test
	public void testResultSetDynamicCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, "carMapping" );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 1, 1 );
		Parameter<String> p2 = storedProcedureQuery.getParameter( 2,String.class );
		storedProcedureQuery.setParameter( p2, "title'1" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsOnly( new Car( 1, "title'1" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultClass() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithEntity" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 1 );
		storedProcedureQuery.setParameter( 3, "title" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsOnly( new Car( 1, "title" ) );
	}

	@Test
	public void testResultSetStaticCallWithResultMapping() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithMapping" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 2 );
		storedProcedureQuery.setParameter( 3, "title'2" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'2" ) );
	}

	@Test
	public void testResultSetStaticCallRaw() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersRaw" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 2 );
		storedProcedureQuery.setParameter( 3, "title'2" );

		@SuppressWarnings("unchecked")
		List<Object[]> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Object[]{ 2, "title'2" } );
	}

	@Test
	public void testResultSetStaticCallWithCodeInjectionForMongoDB() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithMapping" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 2 );
		storedProcedureQuery.setParameter( 3, "title'2\") && returnPositionalParametersWithMapping(2,\"\");" );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'2\") && returnPositionalParametersWithMapping(2,\"\");" ) );

		storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithMapping" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 3 );
		storedProcedureQuery.setParameter( 3, "title'2\\" );

		@SuppressWarnings("unchecked")
		List<Car> listResult2 = storedProcedureQuery.getResultList();
		assertThat( listResult2 ).containsExactly( new Car( 3, "title'2\\" ) );
	}

	@Test
	public void testResultSetStaticCallWithNullStringParameter() throws Exception {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "String" ); // String parameter cannot be null

		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithMapping" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, 2 );
		storedProcedureQuery.setParameter( 3, null );

		@SuppressWarnings("unchecked")
		List<Car> listResult = storedProcedureQuery.getResultList();
		assertThat( listResult ).containsExactly( new Car( 2, "title'2" ) );
	}

	@Test
	public void testResultSetStaticCallWithNullIntegerParameter() throws Exception {
		thrown.expect( IllegalArgumentException.class );
		thrown.expectMessage( "Integer" ); // Integer parameter cannot be null

		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnPositionalParametersWithMapping" );
		// First parameter is void
		storedProcedureQuery.setParameter( 2, null );
		storedProcedureQuery.setParameter( 3, "Whatever" );
		storedProcedureQuery.getResultList();
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

	@Test
	public void testExceptionWhenInvalidTypeIsPassed() throws Exception {
		thrown.expect( IllegalArgumentException.class );

		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, Car.class );
		storedProcedureQuery.registerStoredProcedureParameter( 0, Void.class, ParameterMode.REF_CURSOR );
		storedProcedureQuery.registerStoredProcedureParameter( 1, Integer.class, ParameterMode.IN );
		storedProcedureQuery.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( 1, 1 );
		// Should be a string
		storedProcedureQuery.setParameter( 2, 5 );
		storedProcedureQuery.getResultList();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Car.class, Motorbike.class };
	}
}
