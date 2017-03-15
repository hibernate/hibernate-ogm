/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.INFINISPAN_EMBEDDED;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MAP;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MONGODB;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.NEO4J_EMBEDDED;
import static org.junit.Assert.fail;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that {@link @SkipByDatastoreProvider} given on the class-level is applied.
 *
 * @author Mark Paluch
 */
@SkipByDatastoreProvider({ MAP, INFINISPAN_EMBEDDED, MONGODB, NEO4J_EMBEDDED })
public class SkipByDatastoreProviderClassLevelSelfTest extends OgmTestCase {

	@Test
	public void testWhichAlwaysFails() {
		fail( "This should never be executed" );
	}

	@BeforeClass
	public static void beforeClass() {
		fail( "This should never be executed" );
	}

	@AfterClass
	public static void afterClass() {
		fail( "This should never be executed" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}
}
