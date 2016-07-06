/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.spi.EventContextManagerAwareGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link EventContextManager} service.
 *
 * @author Gunnar Morling
 */
public class EventContextManagerInitiator implements StandardServiceInitiator<EventContextManager> {

	public static final EventContextManagerInitiator INSTANCE = new EventContextManagerInitiator();

	private EventContextManagerInitiator() {
	}

	@Override
	public Class<EventContextManager> getServiceInitiated() {
		return EventContextManager.class;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EventContextManager initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		EventContextManager eventContextManager = new EventContextManager( registry );

		GridDialect gridDialect = registry.getService( GridDialect.class );
		// we don't use GridDialects.hasFacet here as we need to set it even for a {@link ForwardingGridDialect}
		if ( gridDialect instanceof EventContextManagerAwareGridDialect ) {
			( (EventContextManagerAwareGridDialect) gridDialect ).setEventContextManager( eventContextManager );
		}

		return eventContextManager;
	}

}
