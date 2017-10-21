/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PreRemove;
import javax.persistence.Transient;

import org.hibernate.ogm.backendtck.callbacks.PreRemovableBus.PreRemovableBusEventListener;

@Entity
@EntityListeners( PreRemovableBusEventListener.class )
public class PreRemovableBus extends Bus {

	private boolean preRemoveInvoked;
	private boolean preRemoveInvokedByListener;

	public void setPreRemoveInvoked(boolean preRemoveInvoked) {
		this.preRemoveInvoked = preRemoveInvoked;
	}

	public void setPreRemoveInvokedByListener(boolean preRemoveInvokedByListener) {
		this.preRemoveInvokedByListener = preRemoveInvokedByListener;
	}

	@Transient
	public boolean isPreRemoveInvoked() {
		return preRemoveInvoked;
	}

	@Transient
	public boolean isPreRemoveInvokedByListener() {
		return preRemoveInvokedByListener;
	}

	@PreRemove
	public void preRemove() {
		this.preRemoveInvoked = true;
	}

	public static class PreRemovableBusEventListener {

		@PreRemove
		public void preRemove(PreRemovableBus bus) {
			bus.setPreRemoveInvokedByListener( true );
		}
	}
}
