/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.parsing.model.inheritance.singletable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Davide D'Alto
 */
@Entity
@DiscriminatorValue("CMM")
public class CommunityMemberST extends PersonST {

	private String project;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}
}
