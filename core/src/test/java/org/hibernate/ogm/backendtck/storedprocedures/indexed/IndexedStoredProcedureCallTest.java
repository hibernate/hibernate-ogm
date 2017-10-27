/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures.indexed;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.jpa.impl.OgmStoredProcedureQuery;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing call of stored procedures
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@SkipByGridDialect(value = {INFINISPAN,INFINISPAN_REMOTE,NEO4J_EMBEDDED,NEO4J_REMOTE}, comment = "These dialects not supports stored procedures")
@TestForIssue( jiraKey = {"OGM-359"})
public class IndexedStoredProcedureCallTest extends OgmJpaTestCase {

	public static final String TEST_SIMPLE_VALUE_STORED_PROC = "testSimpleValue";
	public static final String TEST_RESULT_SET_STORED_PROC = "testResultSet";
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	/*@Test
	public void dynamicCallOfStoredProcedureWithoutParametersAndResult() throws Exception {
		// no parameters, no results
		final AtomicInteger result = new AtomicInteger( 1 );
		IndexedStoredProcDialect.FUNCTIONS.put( "testproc1", new IndexedStoredProcedure() {
			@Override
			public ClosableIterator<Tuple> execute(Object[] params) {
				result.set( 10 );
				return null;
			}
		} );


		StoredProcedureQuery call1 = em.createStoredProcedureQuery( "testproc1" );
		assertThat( call1 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		assertThat( call1.getParameters() ).hasSize( 0 );
		assertThat( call1.execute() ).isEqualTo( false );
		assertThat( result.get() ).isEqualTo( 10 );
	} */

	/**
	 * Testing a call of stored procedure (function)
	 * @throws Exception
	 */
	@Test
	public void dynamicCallOfStoredProcedureWithParametersAndOneResult() throws Exception {
		StoredProcedureQuery call2 = em.createStoredProcedureQuery( TEST_SIMPLE_VALUE_STORED_PROC );
		assertThat( call2 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		call2.registerStoredProcedureParameter( 0, Integer.class, ParameterMode.IN );
		call2.setParameter( 0, 1 );
		Number singleResult = (Number) call2.getSingleResult();
		assertThat( singleResult ).isNotNull();
		if ( TestHelper.getCurrentDialectType().equals( GridDialectType.MONGODB ) ) {
			//MongoDB can't returns Integer. It returns double :-(
			assertThat( singleResult ).isEqualTo( 1.0d );
		}
		else {
			assertThat( singleResult ).isEqualTo( 1 );
		}
	}
	/**
	 * Testing a call of stored procedure (function)
	 * @throws Exception
	 */
	@Test
	public void dynamicCallOfStoredProcedureWithParametersAndListEntityResult() throws Exception {

		StoredProcedureQuery call3 = em.createStoredProcedureQuery( TEST_RESULT_SET_STORED_PROC, Car.class );
		assertThat( call3 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		call3.registerStoredProcedureParameter( 0,Void.class, ParameterMode.REF_CURSOR );
		call3.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
		call3.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		call3.setParameter( 1, "id" );
		call3.setParameter( 2, "title" );
		List<Car> listResult = call3.getResultList();
		assertThat( listResult ).isNotNull();
		assertThat( listResult.size() ).isEqualTo( 1 );
		assertThat( listResult.get( 0 ) ).isEqualTo( new Car( "id","title" ) );

		StoredProcedureQuery call4 = em.createStoredProcedureQuery( TEST_RESULT_SET_STORED_PROC, "carMapping" );
		assertThat( call4 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		call4.registerStoredProcedureParameter( 0,Void.class, ParameterMode.REF_CURSOR );
		call4.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
		call4.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
		call4.setParameter( 1, "id1" );
		call4.setParameter( 2, "title1" );
		listResult = call4.getResultList();
		assertThat( listResult ).isNotNull();
		assertThat( listResult.size() ).isEqualTo( 1 );
		assertThat( listResult.get( 0 ) ).isEqualTo( new Car( "id1","title1" ) );
	}

	@Test
	public void staticCallOfStoredProcedureWithParametersAndListEntityResult() throws Exception {
		//function with one parameter and returned entity


		StoredProcedureQuery call3 = em.createNamedStoredProcedureQuery( "testproc4_1" );
		assertThat( call3 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		call3.setParameter( 1, "id" );
		call3.setParameter( 2, "title" );
		List<Car> listResult = call3.getResultList();
		assertThat( listResult ).isNotNull();
		assertThat( listResult.size() ).isEqualTo( 1 );
		assertThat( listResult.get( 0 ) ).isEqualTo( new Car( "id", "title" ) );

		StoredProcedureQuery call4 = em.createNamedStoredProcedureQuery( "testproc4_2" );
		assertThat( call4 ).isInstanceOfAny( OgmStoredProcedureQuery.class );
		call4.setParameter( 1, "id1" );
		call4.setParameter( 2, "title1" );
		listResult = call4.getResultList();
		assertThat( listResult ).isNotNull();
		assertThat( listResult.size() ).isEqualTo( 1 );
		assertThat( listResult.get( 0 ) ).isEqualTo( new Car( "id1", "title1" ) );
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {

		Properties properties = info.getProperties();

		if ( TestHelper.getCurrentDialectType().equals( GridDialectType.HASHMAP ) ) {
			// we are in 'core' module. we need to set specified dialect for execute stored procedures
			properties.setProperty( OgmProperties.DATASTORE_PROVIDER, IndexedStoredProcProvider.class.getName() );

			// function with one parameter and result as list of entities
			IndexedStoredProcDialect.FUNCTIONS.put( TEST_RESULT_SET_STORED_PROC, new IndexedStoredProcedure() {

				@Override
				public ClosableIterator<Tuple> execute(Object[] params) {
					List<Tuple> result = new ArrayList<>( 1 );
					Tuple resultTuple = new Tuple();
					resultTuple.put( "id", params[0] );
					resultTuple.put( "title", params[1] );
					result.add( resultTuple );
					return CollectionHelper.newClosableIterator( result );
				}
			} );
			// function with one parameter and returned simple value
			IndexedStoredProcDialect.FUNCTIONS.put( TEST_SIMPLE_VALUE_STORED_PROC, new IndexedStoredProcedure() {

				@Override
				public ClosableIterator<Tuple> execute(Object[] params) {
					List<Tuple> result = new ArrayList<>( 1 );
					Tuple resultTuple = new Tuple();
					resultTuple.put( "result", params[0] );
					result.add( resultTuple );
					return CollectionHelper.newClosableIterator( result );
				}
			} );

		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class };
	}
}
