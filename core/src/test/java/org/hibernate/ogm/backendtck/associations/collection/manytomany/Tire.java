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
public class Tire {
	private TireId tireId;
	private Double size;
	private Set<Car> cars = new HashSet<Car>();


	@EmbeddedId
	public TireId getTireId() {
		return tireId;
	}

	public void setTireId(TireId tireId) {
		this.tireId = tireId;
	}

	public Double getSize() {
		return size;
	}

	public void setSize(Double size) {
		this.size = size;
	}

	@ManyToMany(mappedBy = "tires")
	public Set<Car> getCars() {
		return cars;
	}

	public void setCars(Set<Car> cars) {
		this.cars = cars;
	}

	public static class TireId implements Serializable {
		private String maker;
		private String model;

		public TireId() {
		}

		public TireId(String maker, String model) {
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
