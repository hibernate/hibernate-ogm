/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

import org.hibernate.ogm.backendtck.callbacks.PrePersistableBus.PrePersistableBusEventListener;

@Entity
@EntityListeners(PrePersistableBusEventListener.class)
public class PrePersistableBus extends Bus {

	private boolean prePersisted;
	private boolean prePersistedByListener;

	public void setPrePersisted(boolean persisted) {
		this.prePersisted = persisted;
	}

	public void setPrePersistedByListener(boolean prePersistedByListener) {
		this.prePersistedByListener = prePersistedByListener;
	}

	@Transient
	public boolean isPrePersisted() {
		return prePersisted;
	}

	@Transient
	public boolean isPrePersistedByListener() {
		return prePersistedByListener;
	}

	@PrePersist
	public void prePersist() {
		this.prePersisted = true;
	}

	public static class PrePersistableBusEventListener {

		@PrePersist
		public void prePersist(PrePersistableBus bus) {
			bus.setPrePersistedByListener( true );
		}
	}
}
