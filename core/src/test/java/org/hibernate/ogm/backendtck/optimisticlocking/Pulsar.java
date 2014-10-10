/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

/**
 * @author Gunnar Morling
 */
@Entity
@OptimisticLocking(type = OptimisticLockType.ALL)
@DynamicUpdate
public class Pulsar implements Nameable {

	private String id;
	private String name;
	private double rotationPeriod;

	Pulsar() {
	}

	Pulsar(String id, String name, double rotationPeriod) {
		this.id = id;
		this.name = name;
		this.rotationPeriod = rotationPeriod;
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

	public double getRotationPeriod() {
		return rotationPeriod;
	}

	public void setRotationPeriod(double rotationPeriod) {
		this.rotationPeriod = rotationPeriod;
	}
}
