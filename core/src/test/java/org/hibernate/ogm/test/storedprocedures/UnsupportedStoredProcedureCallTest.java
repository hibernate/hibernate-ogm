/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures;

import java.util.Properties;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the error thrown when stored procedures are used with a dialect that does not support them
 *
 * @see UnsupportedProceduresGridDialect
 * @author Davide D'Alto
 */
public class UnsupportedStoredProcedureCallTest extends OgmJpaTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testExecuteStoredProcedureQuery() throws Exception {
		thrown.expect( UnsupportedOperationException.class );
		thrown.expectMessage( "Grid dialect " + UnsupportedProceduresGridDialect.class.getName() + " does not support server side procedures: procedure 'Whatever' has been called" );

		inTransaction( em -> {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "Whatever" );
			query.execute();
		} );
	}

	@Test
	public void testExecuteUpdateStoredProcedureQuery() throws Exception {
		thrown.expect( UnsupportedOperationException.class );
		thrown.expectMessage( "Grid dialect " + UnsupportedProceduresGridDialect.class.getName() + " does not support server side procedures: procedure 'UpdateOrWhatever' has been called" );

		inTransaction( em -> {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "UpdateOrWhatever" );
			query.executeUpdate();
		} );
	}

	@Test
	public void testCreateNamedStoredProcedureQueryExecute() throws Exception {
		thrown.expect( IllegalArgumentException.class );

		inTransaction( em -> {
			em.createNamedStoredProcedureQuery( "itsNotGonnaWork" );
		} );
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.GRID_DIALECT, UnsupportedProceduresGridDialect.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Puppet.class };
	}

	@Entity
	class Puppet {

		@Id
		String name;
		String color;
	}
}
