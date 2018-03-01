/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Properties;

import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;
import org.junit.Test;

/**
 * This class is supposed to run the tests in {@link NamedParametersStoredProcedureCallTest} on a mock dialect to make
 * sure that the right parameters are passed.
 *
 * @see MockStoredProcedureDialect
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class NamedParametersStoredProcedureCallTest extends org.hibernate.ogm.backendtck.storedprocedures.NamedParametersStoredProcedureCallTest {

	@Test
	public void testStoredProcedureQueryImplementation() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createStoredProcedureQuery( "Whatever" );
		assertThat( storedProcedureQuery ).isInstanceOf( NoSQLProcedureCallImpl.class );
	}

	@Test
	public void testNamedStoredProcedureQueryImplementation() throws Exception {
		StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery( "returnNamedParametersWithEntity" );
		assertThat( storedProcedureQuery ).isInstanceOf( NoSQLProcedureCallImpl.class );
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.GRID_DIALECT, MockStoredProcedureDialect.class.getName() );
	}
}
