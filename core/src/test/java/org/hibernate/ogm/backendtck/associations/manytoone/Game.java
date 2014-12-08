/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.associations.manytoone;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class Game {
	@EmbeddedId
	private GameId id;

	private String name;

	@ManyToOne
	private Court playedOn;

	public GameId getId() {
		return id;
	}

	public void setId(GameId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Court getPlayedOn() {
		return playedOn;
	}

	public void setPlayedOn(Court playedOn) {
		this.playedOn = playedOn;
	}

	public static class GameId implements Serializable {
		private String category;
		@Column(name = "id.gameSequenceNo")
		private int sequenceNo;

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		@Column(name = "id.gameSequenceNo")
		public int getSequenceNo() {
			return sequenceNo;
		}

		public void setSequenceNo(int sequenceNo) {
			this.sequenceNo = sequenceNo;
		}
	}
}
