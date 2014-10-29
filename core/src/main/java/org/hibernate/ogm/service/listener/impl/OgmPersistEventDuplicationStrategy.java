/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.listener.impl;

import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.internal.DefaultPersistOnFlushEventListener;
import org.hibernate.event.internal.DefaultReplicateEventListener;
import org.hibernate.event.internal.DefaultSaveEventListener;
import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.jpa.event.internal.core.JpaMergeEventListener;
import org.hibernate.jpa.event.internal.core.JpaPersistEventListener;
import org.hibernate.jpa.event.internal.core.JpaPersistOnFlushEventListener;
import org.hibernate.jpa.event.internal.core.JpaSaveEventListener;
import org.hibernate.jpa.event.internal.core.JpaSaveOrUpdateEventListener;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;

public class OgmPersistEventDuplicationStrategy implements DuplicationStrategy {

	private final CallbackRegistry callbackRegistry;

	public OgmPersistEventDuplicationStrategy(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public boolean areMatch(Object listener, Object original) {
		boolean match =
				listener.getClass() == OgmDefaultMergeEventListener.class && original.getClass() == DefaultMergeEventListener.class ||
				listener.getClass() == OgmDefaultPersistEventListener.class && original.getClass() == DefaultPersistEventListener.class ||
				listener.getClass() == OgmDefaultPersistOnFlushEventListener.class && original.getClass() == DefaultPersistOnFlushEventListener.class ||
				listener.getClass() == OgmDefaultReplicateEventListener.class && original.getClass() == DefaultReplicateEventListener.class ||
				listener.getClass() == OgmDefaultSaveEventListener.class && original.getClass() == DefaultSaveEventListener.class ||
				listener.getClass() == OgmDefaultSaveOrUpdateEventListener.class && original.getClass() == DefaultSaveOrUpdateEventListener.class ||
				listener.getClass() == OgmDefaultUpdateEventListener.class && original.getClass() == DefaultSaveOrUpdateEventListener.class ||
				listener.getClass() == OgmJpaMergeEventListener.class && original.getClass() == JpaMergeEventListener.class ||
				listener.getClass() == OgmJpaPersistEventListener.class && original.getClass() == JpaPersistEventListener.class ||
				listener.getClass() == OgmJpaPersistOnFlushEventListener.class && original.getClass() == JpaPersistOnFlushEventListener.class ||
				listener.getClass() == OgmJpaSaveEventListener.class && original.getClass() == JpaSaveEventListener.class ||
				listener.getClass() == OgmJpaSaveOrUpdateEventListener.class && original.getClass() == JpaSaveOrUpdateEventListener.class;

		if ( listener instanceof CallbackRegistryConsumer ) {
			( (CallbackRegistryConsumer) listener ).injectCallbackRegistry( callbackRegistry );
		}

		return match;
	}

	@Override
	public Action getAction() {
		return Action.REPLACE_ORIGINAL;
	}
}
