/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.io.Serializable;

import javax.persistence.Embeddable;

/**
 * Identifies a make-up artist.
 *
 * @author Gunnar Morling
 */
@Embeddable
public class MakeUpArtistId implements Serializable {

	private static final long serialVersionUID = 1L;

	private String companyName;
	private String sequenceNo;

	MakeUpArtistId() {
	}

	public MakeUpArtistId(String companyName, String sequenceNo) {
		this.companyName = companyName;
		this.sequenceNo = sequenceNo;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getSequenceNo() {
		return sequenceNo;
	}

	public void setSequenceNo(String sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	@Override
	public String toString() {
		return "TournamentId [companyName=" + companyName + ", sequenceNo=" + sequenceNo + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( companyName == null ) ? 0 : companyName.hashCode() );
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
		MakeUpArtistId other = (MakeUpArtistId) obj;
		if ( companyName == null ) {
			if ( other.companyName != null ) {
				return false;
			}
		}
		else if ( !companyName.equals( other.companyName ) ) {
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
