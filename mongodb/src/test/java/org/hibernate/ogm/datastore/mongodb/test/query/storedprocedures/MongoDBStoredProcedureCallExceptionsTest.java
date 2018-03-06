/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.storedprocedures;

import static org.hibernate.ogm.backendtck.storedprocedures.Car.RESULT_SET_PROC;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
 * Testing call of stored procedures that supports indexed parameters
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = { "OGM-359" })
public class MongoDBStoredProcedureCallExceptionsTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected EntityManager em;

	final Date groundhogDay = new GregorianCalendar( 1993, Calendar.FEBRUARY, 2 ).getTime();

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testExceptionWhenInvalidTypeIsPassedAsNamedParameter() throws Exception {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "org.hibernate.HibernateException: OGM001239: Dialect org.hibernate.ogm.datastore.mongodb.MongoDBDialect does not support named parameters when calling stored procedures" );

		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( RESULT_SET_PROC, Car.class );
		storedProcedureQuery.registerStoredProcedureParameter( "param", Date.class, ParameterMode.IN );
		storedProcedureQuery.setParameter( "param", groundhogDay );
		storedProcedureQuery.getSingleResult();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Car.class };
	}
}
