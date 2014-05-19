/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance;

import javax.persistence.Entity;

@Entity
class Employee extends CommunityMember {
	public String employer;

	public Employee() {
		super();
	}

	public Employee(String name, String employer) {
		super( name );
		this.employer = employer;
	}
}
