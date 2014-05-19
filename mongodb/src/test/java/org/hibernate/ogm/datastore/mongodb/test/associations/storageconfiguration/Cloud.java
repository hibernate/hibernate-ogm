/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations.storageconfiguration;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Cloud {
	private String id;
	private String type;
	private double length;
	private Set<SnowFlake> producedSnowFlakes = new HashSet<SnowFlake>();
	private Set<SnowFlake> backupSnowFlakes = new HashSet<SnowFlake>();

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	@OneToMany
	@JoinTable
	public Set<SnowFlake> getProducedSnowFlakes() {
		return producedSnowFlakes;
	}

	public void setProducedSnowFlakes(Set<SnowFlake> producedSnowFlakes) {
		this.producedSnowFlakes = producedSnowFlakes;
	}

	@OneToMany
	@JoinTable
	public Set<SnowFlake> getBackupSnowFlakes() {
		return backupSnowFlakes;
	}

	public void setBackupSnowFlakes(Set<SnowFlake> backupSnowFlakes) {
		this.backupSnowFlakes = backupSnowFlakes;
	}
}
