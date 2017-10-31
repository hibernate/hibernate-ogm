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

import org.hibernate.ogm.backendtck.callbacks.PostRemovableBus.PostRemovableBusEventListener;

@Entity
@EntityListeners(PostRemovableBusEventListener.class)
public class PostRemovableBus extends Bus {

	private boolean postRemoveInvoked;
	private boolean postRemoveInvokedByListener;

	public void setPostRemoveInvoked(boolean postRemoveInvoked) {
		this.postRemoveInvoked = postRemoveInvoked;
	}

	public boolean isPostRemoveInvoked() {
		return postRemoveInvoked;
	}

	public void setPostRemoveInvokedByListener(boolean postRemoveInvokedByListener) {
		this.postRemoveInvokedByListener = postRemoveInvokedByListener;
	}

	public boolean isPostRemoveInvokedByListener() {
		return postRemoveInvokedByListener;
	}

	@PostRemove
	public void postRemove() {
		this.postRemoveInvoked = true;
	}

	public static class PostRemovableBusEventListener {

		@PostRemove
		public void postRemove(PostRemovableBus bus) {
			bus.setPostRemoveInvokedByListener( true );
		}
	}
}
