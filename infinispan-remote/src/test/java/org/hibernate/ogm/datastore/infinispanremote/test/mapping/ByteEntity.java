/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.mapping;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Owns a {@link Byte} field
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
public class ByteEntity {

	@Id
	private Integer id;

	private Byte counter;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Byte getCounter() {
		return counter;
	}

	public void setCounter(Byte counter) {
		this.counter = counter;
	}
}
