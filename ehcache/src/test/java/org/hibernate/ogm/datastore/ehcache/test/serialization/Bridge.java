/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.test.serialization;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * @author Gunnar Morling
 */
@Entity
public class Bridge {

	private long id;
	private String name;
	private List<Engineer> engineers;

	Bridge() {
	}

	public Bridge(long id, String name, List<Engineer> engineers) {
		this.id = id;
		this.name = name;
		this.engineers = engineers;
	}

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn
	public List<Engineer> getEngineers() {
		return engineers;
	}

	public void setEngineers(List<Engineer> engineers) {
		this.engineers = engineers;
	}

	@Override
	public String toString() {
		return "Bridge [id=" + id + ", name=" + name + ", engineers=" + engineers + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( engineers == null ) ? 0 : engineers.hashCode() );
		result = prime * result + (int) ( id ^ ( id >>> 32 ) );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
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
		Bridge other = (Bridge) obj;
		if ( engineers == null ) {
			if ( other.engineers != null ) {
				return false;
			}
		}
		else if ( !engineers.equals( other.engineers ) ) {
			return false;
		}
		if ( id != other.id ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		return true;
	}
}
