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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Persister(impl = OgmEntityPersister.class)
public class Cavalier {
	@Id @GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	public String getName() { return name; }
	public void setName(String name) {  this.name = name; }
	private String name;

	@OneToOne
	@JoinColumn(name="horse_id")
	public Horse getHorse() { return horse; }
	public void setHorse(Horse horse) {  this.horse = horse; }
	private Horse horse;
}
