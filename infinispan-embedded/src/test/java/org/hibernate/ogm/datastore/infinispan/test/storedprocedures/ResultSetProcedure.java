/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.test.storedprocedures;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.hibernate.ogm.backendtck.storedprocedures.Car;

/**
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class ResultSetProcedure implements Callable<List<Car>> {

	private Integer id;

	private String title;

	public void setId(Integer id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public List<Car> call() {
		return Collections.singletonList( new Car( id, title ) );
	}
}
