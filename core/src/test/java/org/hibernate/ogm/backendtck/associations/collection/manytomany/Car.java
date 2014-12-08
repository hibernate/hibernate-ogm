/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class Car {
	private CarId carId;
	private Integer hp;
	private Set<Tire> tires = new HashSet<Tire>();

	@EmbeddedId
	public CarId getCarId() {
		return carId;
	}

	public void setCarId(CarId carId) {
		this.carId = carId;
	}

	public Integer getHp() {
		return hp;
	}

	public void setHp(Integer hp) {
		this.hp = hp;
	}

	@ManyToMany
	public Set<Tire> getTires() {
		return tires;
	}

	public void setTires(Set<Tire> tires) {
		this.tires = tires;
	}

	public static class CarId implements Serializable {
		private String maker;
		private String model;

		public CarId() {
		}

		public CarId(String maker, String model) {
			this.maker = maker;
			this.model = model;
		}

		public String getMaker() {
			return maker;
		}

		public void setMaker(String maker) {
			this.maker = maker;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

	}
}
