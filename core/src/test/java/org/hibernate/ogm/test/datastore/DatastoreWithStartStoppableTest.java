/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.datastore;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that DatastoreProvider implementing StartStoppable are properly receiving events.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DatastoreWithStartStoppableTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-datastoreobserver.xml", Noise.class );

	@Test
	public void testObserver() throws Exception {
		try {
			final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone-datastoreobserver" );
			Assert.fail( "StartStoppable provider not executed" );
		}
		catch (RuntimeException e) {
			Throwable cause = e;
			do {
				if ( cause.getMessage().equals( "STARTED!" ) ) {
					break;
				}
				cause = cause.getCause();
			} while ( cause != null );
			if ( cause == null ) {
				Assert.fail( "StartStoppable provider not executed" );
			}
		}
	}
}
