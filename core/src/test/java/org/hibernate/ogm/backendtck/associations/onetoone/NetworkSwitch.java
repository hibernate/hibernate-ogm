/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.onetoone;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;

/**
 * @author Mark Paluch
 */
@Entity
public class NetworkSwitch {

	@EmbeddedId
	private MediaId id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "vendor"),
			@JoinColumn(name = "cat")
	})
	private PatchCable patchCable;


	public NetworkSwitch() {
	}

	public NetworkSwitch(MediaId id) {
		this.id = id;
	}

	public MediaId getId() {
		return id;
	}

	public void setId(MediaId id) {
		this.id = id;
	}

	public PatchCable getPatchCable() {
		return patchCable;
	}

	public void setPatchCable(PatchCable patchCable) {
		this.patchCable = patchCable;
	}
}
