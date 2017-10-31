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

import org.hibernate.ogm.backendtck.callbacks.PrePersistableBus.PrePersistableBusEventListener;

@Entity
@EntityListeners(PrePersistableBusEventListener.class)
public class PrePersistableBus extends Bus {

	private boolean prePersisted;
	private boolean prePersistedByListener;

	public boolean isPrePersisted() {
		return prePersisted;
	}

	public void setPrePersisted(boolean prePersisted) {
		this.prePersisted = prePersisted;
	}

	public boolean isPrePersistedByListener() {
		return prePersistedByListener;
	}

	public void setPrePersistedByListener(boolean prePersistedByListener) {
		this.prePersistedByListener = prePersistedByListener;
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
