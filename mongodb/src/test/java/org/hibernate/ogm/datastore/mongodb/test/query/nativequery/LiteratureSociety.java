/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Gunnar Morling
 */
@Entity
public class LiteratureSociety {

	@Id
	private String id;

	private String name;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<Poet> members = new ArrayList<Poet>();

	LiteratureSociety() {
	}

	public LiteratureSociety(String id, String name, Poet... members) {
		this.id = id;
		this.name = name;
		if ( members != null ) {
			for ( Poet poet : members ) {
				this.members.add( poet );
			}
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Poet> getMembers() {
		return members;
	}

	public void setMembers(List<Poet> members) {
		this.members = members;
	}
}
