/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.custom.CustomQuery;

/**
 * An execution plan for a native NoSQL query. This custom implementation will allow us to implement native update
 * queries.
 *
 * @author Gunnar Morling
 */
class NativeNoSqlQueryPlan extends NativeSQLQueryPlan {

	public NativeNoSqlQueryPlan(String sourceQuery, CustomQuery customQuery) {
		super( sourceQuery, customQuery );
	}

	@Override
	public int performExecuteUpdate(QueryParameters queryParameters, SessionImplementor session) throws HibernateException {
		// TODO OGM-519 Implement native update queries
		throw new UnsupportedOperationException( "Native updates not supported yet" );
	}
}
