/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Test;

/**
 * Testing that associations are properly removed.
 *
 * Most NoSql databases has no concept of referential integrity.
 * Thus it will be a task of the user to update both references,
 * in order to keep data consistent.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue( jiraKey = "OGM-1513" )
public class CleanJoinColumnsAfterDeleteReferencedEntitiesTest extends OgmTestCase {

	public static final String SALES_FORCE_ID = "red_hat";
	public static final String SALES_GUY_ID = "eric";

	@Test
	public void testRemoveAssociation() {
		inTransaction( session -> {
			SalesForce force = new SalesForce( SALES_FORCE_ID );
			force.setCorporation( "Red Hat" );
			session.save( force );

			SalesGuy eric = new SalesGuy( SALES_GUY_ID );
			eric.setName( "Eric" );
			eric.setSalesForce( force );
			force.getSalesGuys().add( eric );
			session.save( eric );
		} );

		inTransaction( session -> {
			SalesForce force = session.load( SalesForce.class, SALES_FORCE_ID );
			SalesGuy eric = force.getSalesGuys().iterator().next();
			eric.setSalesForce( null );
			session.remove( force );
		} );

		inTransaction( session -> {
			SalesForce force = new SalesForce( SALES_FORCE_ID );
			force.setCorporation( "Red Hat II" );
			session.save( force );
		} );

		inTransaction( session -> {
			SalesGuy eric = session.load( SalesGuy.class, SALES_GUY_ID );
			assertThat( eric.getSalesForce() ).isNull();
		} );
	}

	@After
	public void clearCaches() {
		deleteAll( SalesGuy.class, SALES_GUY_ID );
		deleteAll( SalesForce.class, SALES_FORCE_ID );

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SalesForce.class, SalesGuy.class };
	}
}
