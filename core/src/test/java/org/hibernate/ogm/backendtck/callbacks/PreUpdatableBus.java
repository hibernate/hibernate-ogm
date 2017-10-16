/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PreUpdate;

import org.hibernate.ogm.backendtck.callbacks.PreUpdatableBus.PreUpdatableBusEventListener;

@Entity
@EntityListeners( PreUpdatableBusEventListener.class )
public class PreUpdatableBus extends Bus {

	private String field;

	private boolean preUpdated;
	private boolean preUpdatedByListener;

	public void setField(String field) {
		this.field = field;
	}

	public void setPreUpdated(boolean preUpdated) {
		this.preUpdated = preUpdated;
	}

	public void setPreUpdatedByListener(boolean preUpdatedByListener) {
		this.preUpdatedByListener = preUpdatedByListener;
	}

	public String getField() {
		return field;
	}

	public boolean isPreUpdated() {
		return preUpdated;
	}

	public boolean isPreUpdatedByListener() {
		return preUpdatedByListener;
	}

	@PreUpdate
	public void preUpdate() {
		this.preUpdated = true;
	}

	public static class PreUpdatableBusEventListener {

		@PreUpdate
		public void preUpdate(PreUpdatableBus bus) {
			bus.setPreUpdatedByListener( true );
		}
	}
}
