/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.ogm.test.associations.manytoone;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Persister(impl = OgmEntityPersister.class)
public class SalesForce {
	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	public String getCorporation() { return corporation; }
	public void setCorporation(String corporation) {  this.corporation = corporation; }
	private String corporation;

	@OneToMany(mappedBy = "salesForce")
	@Persister(impl = OgmCollectionPersister.class)
	public Set<SalesGuy> getSalesGuys() { return salesGuys; }
	public void setSalesGuys(Set<SalesGuy> salesGuys) {  this.salesGuys = salesGuys; }
	private Set<SalesGuy> salesGuys = new HashSet<SalesGuy>();
}
