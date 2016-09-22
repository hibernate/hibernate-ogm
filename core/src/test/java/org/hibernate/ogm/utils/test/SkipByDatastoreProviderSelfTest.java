/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.junit.Assert.fail;

import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Makes sure the {@link SkipByDatastoreProvider} annotation works as expected.
 *
 * @author Gunnar Morling
 */
@RunWith(SkippableTestRunner.class)
public class SkipByDatastoreProviderSelfTest {

	@Test
	@SkipByDatastoreProvider(DatastoreProviderType.MAP)
	public void shouldBeSkipped() {
		fail( "Should not be invoked" );
	}
}
