/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author Gunnar Morling
 */
@Entity
public class Planet implements Nameable {

	private String id;
	private String name;
	private int version;

	Planet() {
	}

	Planet(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Version
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
