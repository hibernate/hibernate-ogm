/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link MultigetGridDialect} service if the current grid dialect implements this dialect
 * facet.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class MultigetGridDialectInitiator implements StandardServiceInitiator<MultigetGridDialect> {

	public static final MultigetGridDialectInitiator INSTANCE = new MultigetGridDialectInitiator();

	private MultigetGridDialectInitiator() {
	}

	@Override
	public Class<MultigetGridDialect> getServiceInitiated() {
		return MultigetGridDialect.class;
	}

	@Override
	public MultigetGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return GridDialects.getDialectFacetOrNull( registry.getService( GridDialect.class ), MultigetGridDialect.class );
	}
}
