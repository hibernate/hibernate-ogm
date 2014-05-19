/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.Query;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.service.Service;


/**
 * There should be a single QueryParserService implementation registered,
 * but we expect to support multiple types using different or hybrid
 * strategies.
 */
public interface QueryParserService extends Service {

	/**
	 * Experimental!
	 * Parameters will very likely need to change.
	 */
	Query getParsedQueryExecutor(OgmSession session, String queryString, Map<String, Object> namedParameters);

}
