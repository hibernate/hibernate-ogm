/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.storedprocedures;

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
public class InfinispanStoredProcedureCallExceptionsTest extends OgmJpaTestCase {

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
	public void testExceptionWhenInvalidProcedureName() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM001113: Cannot instantiate stored procedure 'invalidProcedureName' with resolved name 'invalidProcedureName'." );
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( "invalidProcedureName", Car.class );
		storedProcedureQuery.registerStoredProcedureParameter( "param", Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( "param", 1 );
		storedProcedureQuery.getSingleResult();
	}

	@Test
	public void testExceptionWhenNotRegisteredParameter() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM001114: Cannot set stored procedure 'simpleValueProcedure' parameters '{invalidParam=1}'." );
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( Car.SIMPLE_VALUE_PROC, Integer.class );
		storedProcedureQuery.registerStoredProcedureParameter( "invalidParam", Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( "invalidParam", 1 );
		storedProcedureQuery.getSingleResult();
	}

	@Test
	public void testExceptionWhenProcedureFails() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM001111: Cannot execute stored procedure 'exceptionalProcedure'." );
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( "exceptionalProcedure", Integer.class );
		storedProcedureQuery.registerStoredProcedureParameter( "param", Integer.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( "param", 1 );
		storedProcedureQuery.getSingleResult();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Car.class };
	}
}
