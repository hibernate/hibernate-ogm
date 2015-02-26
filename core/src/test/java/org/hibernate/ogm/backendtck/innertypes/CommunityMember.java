/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.innertypes;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class CommunityMember {

	@Id
	public String name;

	public CommunityMember() {
	}

	public CommunityMember(String name) {
		this.name = name;
	}

	@Entity @Table(name = "employee")
	public static class Employee extends CommunityMember {
		public String employer;

		public Employee() {
			super();
		}

		public Employee(String name, String employer) {
			super( name );
			this.employer = employer;
		}
	}
}
