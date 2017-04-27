/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.query.parsing.impl;

import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.query.spi.BaseQueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author chernolyassv
 */
public class OrientDBBasedQueryParserService extends BaseQueryParserService {

	private static final Log log = LoggerFactory.make();

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	@Override
	public boolean supportsParameters() {
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters) {
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString) {
		throw new UnsupportedOperationException( "Not supported yet." ); // To change body of generated methods, choose
																			// Tools | Templates.
	}

}
