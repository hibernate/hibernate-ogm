/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes Hibernate OGM's {@link QueryTranslatorFactory} implementation.
 *
 * @author Gunnar Morling
 */
public class OgmQueryTranslatorFactoryInitiator implements StandardServiceInitiator<QueryTranslatorFactory> {

	public static final OgmQueryTranslatorFactoryInitiator INSTANCE = new OgmQueryTranslatorFactoryInitiator();

	private OgmQueryTranslatorFactoryInitiator() {
	}

	@Override
	public Class<QueryTranslatorFactory> getServiceInitiated() {
		return QueryTranslatorFactory.class;
	}

	@Override
	public QueryTranslatorFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmQueryTranslatorFactory();
	}
}
