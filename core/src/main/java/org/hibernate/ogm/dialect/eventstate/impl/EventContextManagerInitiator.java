/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link EventContextManager} service.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("rawtypes")
public class EventContextManagerInitiator implements StandardServiceInitiator<EventContextManager> {

	public static final EventContextManagerInitiator INSTANCE = new EventContextManagerInitiator();

	private EventContextManagerInitiator() {
	}

	@Override
	public Class<EventContextManager> getServiceInitiated() {
		return EventContextManager.class;
	}

	@Override
	public EventContextManager initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new EventContextManager();
	}
}
