/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;

/**
 * @author Gunnar Morling
 */
@Entity
public class Enterprise {

	private String id;
	private Map<String, Department> departments = new HashMap<>();
	private Map<String, Integer> revenueByDepartment = new HashMap<>();

	public Enterprise() {
	}

	public Enterprise(String id, Map<String, Department> departments) {
		this.id = id;
		this.departments = departments;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@ElementCollection
	@MapKeyColumn(name = "departmentName")
	public Map<String, Department> getDepartments() {
		return departments;
	}

	public void setDepartments(Map<String, Department> departments) {
		this.departments = departments;
	}

	@ElementCollection
	public Map<String, Integer> getRevenueByDepartment() {
		return revenueByDepartment;
	}

	public void setRevenueByDepartment(Map<String, Integer> revenueByDepartment) {
		this.revenueByDepartment = revenueByDepartment;
	}
}
