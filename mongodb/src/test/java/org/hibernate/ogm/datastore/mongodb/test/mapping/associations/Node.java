/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping.associations;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
class Node {

	@Id
	private String name;

	@OneToMany(mappedBy = "source")
	private Set<NodeLink> children;

	public Node() {
		this.children = new HashSet<>();
	}

	public Node(String name) {
		this();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<NodeLink> getChildren() {
		return children;
	}

	void setChildren(Set<NodeLink> children) {
		this.children = children;
	}
}
