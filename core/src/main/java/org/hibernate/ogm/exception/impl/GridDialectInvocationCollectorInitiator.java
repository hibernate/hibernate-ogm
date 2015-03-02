/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.exception.spi.ErrorHandler;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link GridDialectInvocationCollector} service in case the user has registered a custom
 * {@link ErrorHandler}.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("rawtypes")
public class GridDialectInvocationCollectorInitiator implements StandardServiceInitiator<GridDialectInvocationCollector> {

	public static final GridDialectInvocationCollectorInitiator INSTANCE = new GridDialectInvocationCollectorInitiator();

	private GridDialectInvocationCollectorInitiator() {
	}

	@Override
	public Class<GridDialectInvocationCollector> getServiceInitiated() {
		return GridDialectInvocationCollector.class;
	}

	@Override
	public GridDialectInvocationCollector initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if ( !configurationValues.containsKey( OgmProperties.ERROR_HANDLER ) ) {
			return null;
		}
		else {
			return new GridDialectInvocationCollector();
		}
	}
}
