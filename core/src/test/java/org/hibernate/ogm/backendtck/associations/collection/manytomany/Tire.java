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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( maker == null ) ? 0 : maker.hashCode() );
			result = prime * result + ( ( model == null ) ? 0 : model.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			else if ( obj == null ) {
				return false;
			}
			else if ( getClass() != obj.getClass() ) {
				return false;
			}
			TireId other = (TireId) obj;
			if ( maker == null ) {
				if ( other.maker != null ) {
					return false;
				}
			}
			else if ( !maker.equals( other.maker ) ) {
				return false;
			}
			else if ( model == null ) {
				if ( other.model != null ) {
					return false;
				}
			}
			else if ( !model.equals( other.model ) ) {
				return false;
			}
			return true;
		}

	}
}
