/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures.indexed;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.ParameterMode;
import javax.persistence.Persistence;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.jpa.impl.OgmJpaStoredProcedureQuery;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testing call of stored procedures
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class IndexedStoredProcedureCallTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/stored-proc-ogm.xml", Car.class );

	private EntityManagerFactory emf;
	private EntityManager em;

	@Before
	public void setUp() {
		emf = Persistence.createEntityManagerFactory( "stored-proc-ogm", TestHelper.getDefaultTestSettings() );
		em = emf.createEntityManager();
	}

	@After
	public void tearDown() {
		dropSchemaAndDatabase( emf );
		em.close();
		emf.close();
	}

	@Test
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
		assertThat( call1 ).isInstanceOfAny( OgmJpaStoredProcedureQuery.class );
		assertThat( call1.getParameters() ).hasSize( 0 );
		assertThat( call1.execute() ).isEqualTo( true );
		assertThat( result.get() ).isEqualTo( 10 );
	}

	/**
	 * Testing a call of stored procedure (function)
	 * @throws Exception
	 */
	@Test
	public void dynamicCallOfStoredProcedureWithParametersAndOneResult() throws Exception {
		//function with one parameter and returned simpe value
			IndexedStoredProcDialect.FUNCTIONS.put( "testproc2", new IndexedStoredProcedure() {
				@Override
				public Object execute(Object[] params) {
					return params[0];
				}
			} );
			StoredProcedureQuery call2 = em.createStoredProcedureQuery( "testproc2" );
			assertThat( call2 ).isInstanceOfAny( OgmJpaStoredProcedureQuery.class );
			call2.registerStoredProcedureParameter( 0,Integer.class, ParameterMode.IN );
			call2.setParameter( 0, 1 );
			Integer singleResult = (Integer) call2.getSingleResult();
			assertThat( singleResult ).isNotNull();
			assertThat( singleResult ).isEqualTo( 1 );

	}


}
