/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.test;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(schema = "deposit_deposit", name = "Deposit")
@Entity
public class Deposit {

	@EmbeddedId
	private ObjectId id;
	@Column(name = "PRINTABLENO")
	private String no;
	@Transient
	private ObjectId personId;
	@Column(name = "PERSON_MAJOR")
	private Integer personMajor;
	@Column(name = "PERSON_MINOR")
	private Integer personMinor;

	public Deposit(ObjectId id, String no, ObjectId personId) {
		this.id = id;
		this.no = no;
		if (personId != null) {
			this.personMajor = personId.getMajorId();
			this.personMinor = personId.getMinorId();
		}
	}

	public Deposit() {

	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getNo() {
		return no;
	}

	public void setNo(String no) {
		this.no = no;
	}

	public ObjectId getPersonId() {
		if (personMajor != null && personMinor != null) {
			personId = new ObjectId(id.getMegaId(), personMajor, personMinor);
		}
		return personId;
	}

	public void setPersonId(ObjectId personId) {
		this.personId = personId;
		if (personId != null) {
			personMajor = personId.getMajorId();
			personMinor = personId.getMinorId();
		}
	}

	public Integer getPersonMajor() {
		return personMajor;
	}

	public void setPersonMajor(Integer personMajor) {
		this.personMajor = personMajor;
	}

	public Integer getPersonMinor() {
		return personMinor;
	}

	public void setPersonMinor(Integer personMinor) {
		this.personMinor = personMinor;
	}

}
