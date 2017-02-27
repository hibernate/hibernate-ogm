/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import org.junit.rules.ExternalResource;

/**
 * A JUnit rule to make sure all tests using the Byteman Helper
 * org.hibernate.search.testsupport.BytemanHelper
 * do properly cleanup the static shared state.
 */
public class BytemanHelperStateCleanup extends ExternalResource {

	@Override
	protected void after() {
		BytemanHelper.resetEventStack();
		BytemanHelper.resetCounters();
	}

}
