/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.jpa.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@Entity
@IdClass(PassportPK.class)
public class Passport implements Serializable {

	@Id
	private int seria;
	@Id
	private long number;

	private String fio;

	public int getSeria() {
		return seria;
	}

	public void setSeria(int seria) {
		this.seria = seria;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public String getFio() {
		return fio;
	}

	public void setFio(String fio) {
		this.fio = fio;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + this.seria;
		hash = 19 * hash + (int) ( this.number ^ ( this.number >>> 32 ) );
		return hash;
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
		final Passport other = (Passport) obj;
		if ( this.seria != other.seria ) {
			return false;
		}
		if ( this.number != other.number ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Passport{" + "seria=" + seria + ", number=" + number + ", fio=" + fio + '}';
	}

}
