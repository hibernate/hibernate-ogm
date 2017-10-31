/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PostPersist;

import org.hibernate.ogm.backendtck.callbacks.PostPersistableBus.PostPersistableBusEventListener;

@Entity
@EntityListeners(PostPersistableBusEventListener.class)
public class PostPersistableBus extends Bus {

	private boolean postPersisted;
	private boolean postPersistedByListener;

	public boolean isPostPersisted() {
		return postPersisted;
	}

	public void setPostPersisted(boolean postPersisted) {
		this.postPersisted = postPersisted;
	}

	public boolean isPostPersistedByListener() {
		return postPersistedByListener;
	}

	public void setPostPersistedByListener(boolean postPersistedByListener) {
		this.postPersistedByListener = postPersistedByListener;
	}

	@PostPersist
	public void postPersist() {
		this.postPersisted = true;
	}

	public static class PostPersistableBusEventListener {

		@PostPersist
		public void postPersist(PostPersistableBus bus) {
			bus.setPostPersistedByListener( true );
		}
	}

}
