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
import javax.persistence.Transient;

import org.hibernate.ogm.backendtck.callbacks.PostPersistableBus.PostPersistableBusEventListener;

@Entity
@EntityListeners(PostPersistableBusEventListener.class)
public class PostPersistableBus extends Bus {

	private boolean isPostPersisted;
	private boolean isPostPersistedByListener;

	public void setPostPersisted(boolean postPersisted) {
		isPostPersisted = postPersisted;
	}

	public void setPostPersistedByListener(boolean postPersistedByListener) {
		isPostPersistedByListener = postPersistedByListener;
	}

	@Transient
	public boolean isPostPersisted() {
		return isPostPersisted;
	}

	@Transient
	public boolean isPostPersistedByListener() {
		return isPostPersistedByListener;
	}

	@PostPersist
	public void postPersist() {
		this.isPostPersisted = true;
	}

	public static class PostPersistableBusEventListener {

		@PostPersist
		public void postPersist(PostPersistableBus bus) {
			bus.setPostPersistedByListener( true );
		}
	}

}
