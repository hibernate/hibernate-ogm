/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class QtyContents {

	private String avgMeasure;
	private String quantity;

	public QtyContents() {
	}

	public QtyContents(String average, String quantity) {
		this.avgMeasure = average;
		this.quantity = quantity;
	}

	public String getAvgMeasure() {
		return avgMeasure;
	}

	public void setAvgMeasure(String avgMeasure) {
		this.avgMeasure = avgMeasure;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "(" + avgMeasure + ", " + quantity + ")";
	}

	@Override
	public int hashCode() {
		return Objects.hash( avgMeasure, quantity );
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
		QtyContents other = (QtyContents) obj;
		return Objects.equals( avgMeasure, other.avgMeasure )
				&& Objects.equals( quantity, other.quantity );
	}
}
