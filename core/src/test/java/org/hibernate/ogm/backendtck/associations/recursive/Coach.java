/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.recursive;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class Coach {

	@Id
	private Integer number;

	@OneToOne(fetch = FetchType.LAZY)
	private Coach previous;

	@OneToOne(mappedBy = "previous")
	private Coach next;

	public Coach() {
	}

	public Coach(Integer number) {
		this.number = number;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Coach getPrevious() {
		return previous;
	}

	public void setPrevious(Coach previous) {
		this.previous = previous;
	}

	public Coach getNext() {
		return next;
	}

	public void setNext(Coach next) {
		this.next = next;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( next == null ) ? 0 : next.hashCode() );
		result = prime * result + ( ( number == null ) ? 0 : number.hashCode() );
		result = prime * result + ( ( previous == null ) ? 0 : previous.hashCode() );
		return result;
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
		Coach other = (Coach) obj;
		if ( next == null ) {
			if ( other.next != null ) {
				return false;
			}
		}
		else if ( !next.equals( other.next ) ) {
			return false;
		}
		if ( number == null ) {
			if ( other.number != null ) {
				return false;
			}
		}
		else if ( !number.equals( other.number ) ) {
			return false;
		}
		if ( previous == null ) {
			if ( other.previous != null ) {
				return false;
			}
		}
		else if ( !previous.equals( other.previous ) ) {
			return false;
		}
		return true;
	}
}
