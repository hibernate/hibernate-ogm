/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link IdentityColumnAwareGridDialect} service if the current grid dialect implements this dialect
 * facet.
 *
 * @author Gunnar Morling
 */
public class IdentityColumnAwareGridDialectInitiator implements StandardServiceInitiator<IdentityColumnAwareGridDialect> {

	public static final IdentityColumnAwareGridDialectInitiator INSTANCE = new IdentityColumnAwareGridDialectInitiator();

	private IdentityColumnAwareGridDialectInitiator() {
	}

	@Override
	public Class<IdentityColumnAwareGridDialect> getServiceInitiated() {
		return IdentityColumnAwareGridDialect.class;
	}

	@Override
	public IdentityColumnAwareGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return GridDialects.getDialectFacetOrNull( registry.getService( GridDialect.class ), IdentityColumnAwareGridDialect.class );
	}
}
