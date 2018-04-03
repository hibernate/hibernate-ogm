/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures;

import org.infinispan.tasks.ServerTask;
import org.infinispan.tasks.TaskContext;

/**
 * @author The Viet Nguyen
 */
public class ExceptionalProcedure implements ServerTask<Void> {

	@Override
	public void setTaskContext(TaskContext taskContext) {
	}

	@Override
	public Void call() throws Exception {
		throw new Exception( "Failure! This is supposed to happen for tests, don't worry" );
	}

	@Override
	public String getName() {
		return "exceptionalProcedure";
	}
}
