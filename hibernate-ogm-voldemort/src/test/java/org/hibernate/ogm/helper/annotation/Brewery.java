/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.helper.annotation;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Brewery {
	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	@Column(name = "brewery_pk")
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	@OneToMany @JoinColumn(name = "brewery_id")
	@Cascade( { CascadeType.PERSIST, CascadeType.SAVE_UPDATE} )
	public Set<Beer> getBeers() { return beers; }
	public void setBeers(Set<Beer> beers) {  this.beers = beers; }
	private Set<Beer> beers = new HashSet<Beer>(  );
}
