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
package org.hibernate.ogm.test.associations.collection.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmCollectionPersister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Persister(impl = OgmEntityPersister.class)
public class User {
	@Id
	@GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	public String getId() { return id; }
	public void setId(String id) {  this.id = id; }
	private String id;

	@OneToMany
	@JoinTable(name = "User_Address")
	@MapKeyColumn(name = "nick")
	@Persister(impl = OgmCollectionPersister.class)
	public Map<String,Address> getAddresses() { return addresses; }
	public void setAddresses(Map<String,Address> addresses) {  this.addresses = addresses; }
	private Map<String,Address> addresses = new HashMap<String,Address>();

	@ElementCollection
	@JoinTable(name = "Nicks", joinColumns = @JoinColumn(name = "user_id") )
	@Persister(impl = OgmCollectionPersister.class)
	public Set<String> getNicknames() { return nicknames; }
	public void setNicknames(Set<String> nicknames) {  this.nicknames = nicknames; }
	private Set<String> nicknames = new HashSet<String>();

}
