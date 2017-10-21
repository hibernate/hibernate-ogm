/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PostUpdate;

import org.hibernate.ogm.backendtck.callbacks.PostUpdatableBus.PostUpdatableBusEventListener;

@Entity
@EntityListeners( PostUpdatableBusEventListener.class )
public class PostUpdatableBus extends Bus {

	private String field;

	private boolean postUpdated;
	private boolean postUpdatedByListener;

	public void setField(String field) {
		this.field = field;
	}

	public void setPostUpdated(boolean postUpdated) {
		this.postUpdated = postUpdated;
	}

	public void setPostUpdatedByListener(boolean postUpdatedByListener) {
		this.postUpdatedByListener = postUpdatedByListener;
	}

	public String getField() {
		return field;
	}

	public boolean isPostUpdated() {
		return postUpdated;
	}

	public boolean isPostUpdatedByListener() {
		return postUpdatedByListener;
	}

	@PostUpdate
	public void postUpdate() {
		this.postUpdated = true;
	}

	public static class PostUpdatableBusEventListener {

		@PostUpdate
		public void postUpdate(PostUpdatableBus bus) {
			bus.setPostUpdatedByListener( true );
		}
	}
}
