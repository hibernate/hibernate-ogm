/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.associations.manytoone;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class Court {
	@EmbeddedId
	private CourtId id;

	private String name;

	@OneToMany(mappedBy = "playedOn")
	private Set<Game> games = new HashSet<Game>();

	public CourtId getId() {
		return id;
	}

	public void setId(CourtId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Game> getGames() {
		return games;
	}

	public void setGames(Set<Game> games) {
		this.games = games;
	}

	public static class CourtId implements Serializable {
		private String countryCode;
		private int sequenceNo;

		public CourtId() {
		}

		public CourtId(String countryCode, int sequenceNo) {
			this.countryCode = countryCode;
			this.sequenceNo = sequenceNo;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		public int getSequenceNo() {
			return sequenceNo;
		}

		public void setSequenceNo(int sequenceNo) {
			this.sequenceNo = sequenceNo;
		}
	}
}
