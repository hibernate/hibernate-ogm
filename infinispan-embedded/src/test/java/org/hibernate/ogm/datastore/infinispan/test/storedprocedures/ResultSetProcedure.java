/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.test.storedprocedures;

import org.hibernate.ogm.backendtck.storedprocedures.Car;

import org.infinispan.util.function.SerializableCallable;

/**
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class ResultSetProcedure implements SerializableCallable<Car> {

	private Integer id;

	private String title;

	public void setId(Integer id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public Car call() {
		return new Car( id, title );
	}
}
