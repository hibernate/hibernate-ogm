/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.spi.QueryableGridDialect;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link QueryableGridDialect} service if the current grid dialect implements this dialect facet.
 *
 * @author Gunnar Morling
 */
public class QueryableGridDialectInitiator implements StandardServiceInitiator<QueryableGridDialect> {

	public static final QueryableGridDialectInitiator INSTANCE = new QueryableGridDialectInitiator();

	private QueryableGridDialectInitiator() {
	}

	@Override
	public Class<QueryableGridDialect> getServiceInitiated() {
		return QueryableGridDialect.class;
	}

	@Override
	public QueryableGridDialect initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		GridDialect gridDialect = registry.getService( GridDialect.class );
		return gridDialect instanceof QueryableGridDialect ? (QueryableGridDialect) gridDialect : null;
	}
}
