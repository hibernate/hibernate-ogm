/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;

@Entity
public class NodeLink {

	private String id;

	private Node source;
	private Node target;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Type(type = "objectid")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ManyToOne
	public Node getSource() {
		return source;
	}

	void setSource(Node source) {
		this.source = source;
	}

	public void assignSource(Node source) {
		setSource( source );
		source.getChildren().add( this );
	}

	@ManyToOne
	public Node getTarget() {
		return target;
	}

	void setTarget(Node target) {
		this.target = target;
	}

	public void assignTarget(Node target) {
		setTarget( target );
	}


	@Override
	public String toString() {
		return "NodeLink(" + id + "):  " + source + " to " + target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		result = prime * result + ( ( source == null ) ? 0 : source.hashCode() );
		result = prime * result + ( ( target == null ) ? 0 : target.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		NodeLink other = (NodeLink) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		if ( source == null ) {
			if ( other.source != null ) {
				return false;
			}
		}
		else if ( !source.equals( other.source ) ) {
			return false;
		}
		if ( target == null ) {
			if ( other.target != null ) {
				return false;
			}
		}
		else if ( !target.equals( other.target ) ) {
			return false;
		}
		return true;
	}
}
