/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link OptimisticLockingAwareGridDialect} service if the current grid dialect implements this dialect
 * facet.
 *
 * @author Gunnar Morling
 */
public class OptimisticLockingAwareGridDialectInitiator implements StandardServiceInitiator<OptimisticLockingAwareGridDialect> {

	public static final OptimisticLockingAwareGridDialectInitiator INSTANCE = new OptimisticLockingAwareGridDialectInitiator();

	private OptimisticLockingAwareGridDialectInitiator() {
	}

	@Override
	public Class<OptimisticLockingAwareGridDialect> getServiceInitiated() {
		return OptimisticLockingAwareGridDialect.class;
	}

	@Override
	public OptimisticLockingAwareGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return GridDialects.getDialectFacetOrNull( registry.getService( GridDialect.class ), OptimisticLockingAwareGridDialect.class );
	}
}
