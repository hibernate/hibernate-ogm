/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.associations.collection.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class Race {
	@EmbeddedId
	private RaceId raceId;

	@OrderColumn(name = "ranking")
	@OneToMany @JoinTable(name = "Race_Runners")
	private List<Runner> runnersByArrival = new ArrayList<Runner>();

	public RaceId getRaceId() {
		return raceId;
	}

	public void setRaceId(RaceId raceId) {
		this.raceId = raceId;
	}

	public List<Runner> getRunnersByArrival() {
		return runnersByArrival;
	}

	public void setRunnersByArrival(List<Runner> runnersByArrival) {
		this.runnersByArrival = runnersByArrival;
	}

	public static class RaceId implements Serializable {
		private int federationSequence;
		private int federationDepartment;

		public RaceId() {
		}

		public RaceId(int federationSequence, int federationDepartment) {
			this.federationSequence = federationSequence;
			this.federationDepartment = federationDepartment;
		}

		public int getFederationSequence() {
			return federationSequence;
		}

		public void setFederationSequence(int federationSequence) {
			this.federationSequence = federationSequence;
		}

		public int getFederationDepartment() {
			return federationDepartment;
		}

		public void setFederationDepartment(int federationDepartment) {
			this.federationDepartment = federationDepartment;
		}
	}
}
