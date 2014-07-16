/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.service.Service;

/**
 * There should be a single QueryParserService implementation registered,
 * but we expect to support multiple types using different or hybrid
 * strategies.
 */
@Experimental("This contract is still under active development")
public interface QueryParserService extends Service {

	boolean supportsParameters();

	// TODO: Should SF be injected during construction?
	QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters);

	QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString);
}
