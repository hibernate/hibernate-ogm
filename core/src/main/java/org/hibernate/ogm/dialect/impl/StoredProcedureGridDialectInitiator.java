/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureAwareGridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class StoredProcedureGridDialectInitiator implements StandardServiceInitiator<StoredProcedureAwareGridDialect> {

	public static final StoredProcedureGridDialectInitiator INSTANCE = new StoredProcedureGridDialectInitiator();

	private StoredProcedureGridDialectInitiator() {
	}

	@Override
	public Class<StoredProcedureAwareGridDialect> getServiceInitiated() {
		return StoredProcedureAwareGridDialect.class;
	}

	@Override
	public StoredProcedureAwareGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return GridDialects.getDialectFacetOrNull( registry.getService( GridDialect.class ), StoredProcedureAwareGridDialect.class );
	}
}
