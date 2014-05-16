/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.onetoone;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.Cascade;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Wheel {
	private String id;
	private double diameter;
	private Vehicule vehicule;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getDiameter() {
		return diameter;
	}

	public void setDiameter(double diameter) {
		this.diameter = diameter;
	}

	@OneToOne(cascade = CascadeType.PERSIST)
	@PrimaryKeyJoinColumn
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@MapsId
	public Vehicule getVehicule() {
		return vehicule;
	}

	public void setVehicule(Vehicule vehicule) {
		this.vehicule = vehicule;
	}
}
