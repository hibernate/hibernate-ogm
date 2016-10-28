/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping.associations;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
class NodeLink {

	@Id
	private String id;

	@ManyToOne
	private Node source;

	@ManyToOne
	private Node target;

	public NodeLink() {
	}

	public NodeLink(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Node getSource() {
		return source;
	}

	void setSource(Node source) {
		this.source = source;
	}

	public Node getTarget() {
		return target;
	}

	void setTarget(Node target) {
		this.target = target;
	}

	public void assignTarget(Node target) {
		setTarget( target );
	}
}
