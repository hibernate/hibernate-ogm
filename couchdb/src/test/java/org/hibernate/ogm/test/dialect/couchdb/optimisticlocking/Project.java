/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.dialect.couchdb.optimisticlocking;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.ogm.options.generic.document.AssociationStorage;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * @author Gunnar Morling
 */
@Entity
public class Project {

	private String id;
	private String name;
	private String revision;
	private Set<Contributor> members = new HashSet<Contributor>();
	private Set<User> users = new HashSet<User>();
	private ProjectGroup projectGroup;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "_rev")
	@Version
	@Generated(GenerationTime.ALWAYS)
	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany
	@OptimisticLock(excluded = true)
	@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
	// need to use association documents as otherwise association updates would modify the entity document and thus
	// implicitly change the revision in the data store
	public Set<Contributor> getMembers() {
		return members;
	}

	public void setMembers(Set<Contributor> members) {
		this.members = members;
	}

	@ManyToMany
	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	@ManyToOne
	public ProjectGroup getProjectGroup() {
		return projectGroup;
	}

	public void setProjectGroup(ProjectGroup projectGroup) {
		this.projectGroup = projectGroup;
	}

	@Override
	public String toString() {
		Set<String> contributorIds = new HashSet<String>();
		for ( Contributor contributor : members ) {
			contributorIds.add( contributor.getId() );
		}

		Set<String> userIds = new HashSet<String>();
		for ( User user : users ) {
			userIds.add( user.getId() );
		}

		return "Project [id=" + id + ", name=" + name + ", revision=" + revision + ", members=[" + StringHelper.join( contributorIds, ", " ) + "]"
				+ ", users=[" + StringHelper.join( userIds, ", " ) + "]]";
	}
}
