/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.compositeid;

import java.io.Serializable;

import javax.persistence.Embeddable;

/**
 * Identifies a tournament.
 *
 * @author Gunnar Morling
 */
@Embeddable
public class TournamentId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String countryCode;
	private String sequenceNo;

	TournamentId() {
	}

	public TournamentId(String countryCode, String sequenceNo) {
		this.countryCode = countryCode;
		this.sequenceNo = sequenceNo;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getSequenceNo() {
		return sequenceNo;
	}

	public void setSequenceNo(String sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	@Override
	public String toString() {
		return "TournamentId [countryCode=" + countryCode + ", sequenceNo=" + sequenceNo + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( countryCode == null ) ? 0 : countryCode.hashCode() );
		result = prime * result + ( ( sequenceNo == null ) ? 0 : sequenceNo.hashCode() );
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
		TournamentId other = (TournamentId) obj;
		if ( countryCode == null ) {
			if ( other.countryCode != null ) {
				return false;
			}
		}
		else if ( !countryCode.equals( other.countryCode ) ) {
			return false;
		}
		if ( sequenceNo == null ) {
			if ( other.sequenceNo != null ) {
				return false;
			}
		}
		else if ( !sequenceNo.equals( other.sequenceNo ) ) {
			return false;
		}
		return true;
	}
}
