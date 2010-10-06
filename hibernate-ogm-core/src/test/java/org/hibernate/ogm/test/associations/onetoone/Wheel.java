/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.test.associations.onetoone;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Persister(impl = OgmEntityPersister.class)
public class Wheel {
	@Id
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	public double getDiameter() { return diameter; }
	public void setDiameter(double diameter) {  this.diameter = diameter; }
	private double diameter;

	@OneToOne( cascade = CascadeType.PERSIST ) @PrimaryKeyJoinColumn
	@Cascade( org.hibernate.annotations.CascadeType.SAVE_UPDATE )
	@MapsId
	public Vehicule getVehicule() { return vehicule; }
	public void setVehicule(Vehicule vehicule) {  this.vehicule = vehicule; }
	private Vehicule vehicule;
}
