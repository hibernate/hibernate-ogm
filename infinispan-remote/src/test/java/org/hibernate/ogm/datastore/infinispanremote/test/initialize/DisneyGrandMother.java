/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

/**
 * @author Davide D'Alto
 */
@Entity
public class DisneyGrandMother {

	private String id;
	private List<DisneyGrandChild> grandChildren = new ArrayList<DisneyGrandChild>();

	public DisneyGrandMother() {
	}

	public DisneyGrandMother(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ElementCollection
	@OrderColumn(name = "birthorder")
	public List<DisneyGrandChild> getGrandChildren() {
		return grandChildren;
	}

	public void setGrandChildren(List<DisneyGrandChild> children) {
		this.grandChildren = children;
	}
}
