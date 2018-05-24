/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures;

import javax.persistence.EntityManagerFactory;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.hibernate.ogm.backendtck.storedprocedures.NamedParametersStoredProcedureCallTest;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteJpaServerRunner;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper;
import org.hibernate.ogm.utils.TestEntityManagerFactory;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing call of stored procedures
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
@TestForIssue(jiraKey = { "OGM-1430" })
@RunWith(InfinispanRemoteJpaServerRunner.class)
public class InfinispanNamedParametersStoredProcedureCallTest extends NamedParametersStoredProcedureCallTest {

	@TestEntityManagerFactory(scope = TestEntityManagerFactory.Scope.TEST_CLASS)
	private static EntityManagerFactory globalFactory;

	@BeforeClass
	public static void setUpAll() throws Exception {
		InfinispanRemoteTestHelper.registerScriptStoredProcedures( ( (SessionFactory) globalFactory ) );
	}

	@AfterClass
	public static void tearDownAll() {
		InfinispanRemoteTestHelper.clearScriptStoredProcedures( ( (SessionFactory) globalFactory ) );
	}

	@Test
	public void testExceptionWhenUsePositionalParameters() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM000094" );
		inTransaction( em -> {
			StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( Car.SIMPLE_VALUE_PROC, Car.class );
			storedProcedureQuery.registerStoredProcedureParameter( 0, Integer.class, ParameterMode.IN );
			storedProcedureQuery.setParameter( 0, 1 );
			storedProcedureQuery.getSingleResult();
		} );
	}
}
