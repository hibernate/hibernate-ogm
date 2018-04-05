/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.test.storedprocedures;

import java.util.concurrent.Callable;

/**
 * Store procedures should implement {@link Callable}.
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class InvalidStoredProcedure implements Runnable {

	@Override
	public void run() {
	}
}
