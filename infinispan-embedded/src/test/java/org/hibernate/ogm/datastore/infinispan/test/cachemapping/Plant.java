/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.cachemapping;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

/**
 * @author Gunnar Morling
 */
@Entity
public class Plant {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "myIdGenerator")
	@TableGenerator(name = "myIdGenerator", table = "myIds")
	private Long id;

	private int height;

	Plant() {
	}

	public Plant(int height) {
		this.height = height;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
