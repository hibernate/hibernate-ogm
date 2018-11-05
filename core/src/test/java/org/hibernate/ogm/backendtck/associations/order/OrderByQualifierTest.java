/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.order;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.OrderBy;

import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Test;

/**
 * Tests the use of {@link OrderBy} qualifier, on load association list.
 *
 * @author Fabio Massimo Ercoli
 */
public class OrderByQualifierTest extends OgmTestCase {

	public static final String PROJECT_NAME = "Hibernate OGM";

	public static final String UNCLEBOB_ID = "unclebob";
	public static final String DDALTO_ID = "ddalto";
	public static final String TELEGRAPH_4_ETERNITY_ID = "telegraph4eternity";
	public static final String FAX_4_EVER_ID = "fax4ever";
	public static final String PHONE_4_INDUCTION_ID = "phone4induction";

	public static final Contributor UNCLEBOB = new Contributor( UNCLEBOB_ID, 370000 );
	public static final Contributor DDALTO = new Contributor( DDALTO_ID, 23999 );
	public static final Contributor TELEGRAPH_4_ETERNITY = new Contributor( TELEGRAPH_4_ETERNITY_ID, 5 );
	public static final Contributor FAX_4_EVER = new Contributor( FAX_4_EVER_ID, 27 );
	public static final Contributor PHONE_4_INDUCTION = new Contributor( PHONE_4_INDUCTION_ID, 9 );

	@Test
	public void testOrderByQualifier() {
		OpenSourceProject project = new OpenSourceProject();
		project.setName( PROJECT_NAME );

		// that's the original order:
		project.addContributor( UNCLEBOB );
		project.addContributor( DDALTO );
		project.addContributor( TELEGRAPH_4_ETERNITY );
		project.addContributor( FAX_4_EVER );
		project.addContributor( PHONE_4_INDUCTION );

		inTransaction( session -> persistAll( session, UNCLEBOB, DDALTO, TELEGRAPH_4_ETERNITY, FAX_4_EVER, PHONE_4_INDUCTION, project ) );

		inTransaction( session -> {
			OpenSourceProject load = session.load( OpenSourceProject.class, PROJECT_NAME );

			// TODO: Switch to AssertJ, so that we will able to do something like that:
			// assertThat( load.getContributors() ).containsExactly( ... );
			// Opened issue: https://hibernate.atlassian.net/browse/OGM-1438

			assertThat( load.getContributors() ).containsOnly( DDALTO, FAX_4_EVER, PHONE_4_INDUCTION, TELEGRAPH_4_ETERNITY, UNCLEBOB );
			assertThat( load.getContributors() ).hasSize( 5 );
			assertThat( load.getContributors().get( 0 ) ).isEqualTo( DDALTO );
			assertThat( load.getContributors().get( 1 ) ).isEqualTo( FAX_4_EVER );
			assertThat( load.getContributors().get( 2 ) ).isEqualTo( PHONE_4_INDUCTION );
			assertThat( load.getContributors().get( 3 ) ).isEqualTo( TELEGRAPH_4_ETERNITY );
			assertThat( load.getContributors().get( 4 ) ).isEqualTo( UNCLEBOB );
		} );
	}

	@After
	public void tearDown() {
		deleteAll( Contributor.class, UNCLEBOB_ID, UNCLEBOB_ID, DDALTO_ID, TELEGRAPH_4_ETERNITY_ID, FAX_4_EVER_ID, PHONE_4_INDUCTION_ID );
		deleteAll( OpenSourceProject.class, PROJECT_NAME );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Contributor.class, OpenSourceProject.class };
	}
}
