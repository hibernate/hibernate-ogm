/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PostRemove;
import javax.persistence.Transient;

import org.hibernate.ogm.backendtck.callbacks.PostRemovableBus.PostRemovableBusEventListener;

@Entity
@EntityListeners( PostRemovableBusEventListener.class )
public class PostRemovableBus extends Bus {

	private boolean isPostRemoveInvoked;
	private boolean isPostRemoveInvokedByListener;

	public void setPostRemoveInvoked(boolean postRemoveInvoked) {
		isPostRemoveInvoked = postRemoveInvoked;
	}

	public void setPostRemoveInvokedByListener(boolean postRemoveInvokedByListener) {
		isPostRemoveInvokedByListener = postRemoveInvokedByListener;
	}

	@Transient
	public boolean isPostRemoveInvoked() {
		return isPostRemoveInvoked;
	}

	@Transient
	public boolean isPostRemoveInvokedByListener() {
		return isPostRemoveInvokedByListener;
	}

	@PostRemove
	public void postRemove() {
		this.isPostRemoveInvoked = true;
	}

	public static class PostRemovableBusEventListener {

		@PostRemove
		public void postRemove(PostRemovableBus bus) {
			bus.setPostRemoveInvokedByListener( true );
		}
	}
}
