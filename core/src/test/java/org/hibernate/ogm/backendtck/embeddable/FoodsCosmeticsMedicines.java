/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class FoodsCosmeticsMedicines {

	@Id
	private String name;

	@ElementCollection
	private List<QtyContents> qtyContentsList = new ArrayList<>();

	public FoodsCosmeticsMedicines() {
	}

	public FoodsCosmeticsMedicines(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public java.util.List<QtyContents> getQtyContentsList() {
		return qtyContentsList;
	}

	public void setQtyContentsList(java.util.List<QtyContents> qtyContentsList) {
		this.qtyContentsList = qtyContentsList;
	}

	@Override
	public String toString() {
		return "[" + name + " " + qtyContentsList + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		FoodsCosmeticsMedicines other = (FoodsCosmeticsMedicines) obj;
		return Objects.equals( name, other.name );
	}
}
