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
public class SimpleValueProcedure implements ServerTask<Integer> {

	private int id;

	@Override
	public void setTaskContext(TaskContext taskContext) {
		id = taskContext.getParameters()
				.map( p -> p.get( "param" ) )
				.map( Integer.class::cast )
				.orElseThrow( () -> new RuntimeException( "missing parameter 'param'" ) );
	}

	@Override
	public Integer call() throws Exception {
		return id;
	}

	@Override
	public String getName() {
		return "simpleValueProcedure";
	}
}
