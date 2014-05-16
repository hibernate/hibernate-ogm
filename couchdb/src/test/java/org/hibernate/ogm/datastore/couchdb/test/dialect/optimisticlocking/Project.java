/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect.optimisticlocking;

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
import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
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
