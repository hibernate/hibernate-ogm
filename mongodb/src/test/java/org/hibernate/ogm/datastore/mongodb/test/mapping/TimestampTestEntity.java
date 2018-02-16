/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import java.sql.Timestamp;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

/**
 * Test entity containing the timestamp data type that data store needs to handle.
 *
 * @author Pavel Novikov
 */
@Entity
public class TimestampTestEntity {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	// Timestamp type
	private Timestamp creationDateAndTime;

	public Timestamp getCreationDateAndTime() {
		return creationDateAndTime;
	}

	public void setCreationDateAndTime(Timestamp creationDateAndTime) {
		this.creationDateAndTime = creationDateAndTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
