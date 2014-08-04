/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.GridDialectLogger;
import org.hibernate.ogm.dialect.spi.QueryableGridDialect;
import org.hibernate.ogm.query.spi.BackendQuery;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.ogm.util.impl.CoreLogCategories;
import org.hibernate.ogm.util.impl.Log;
import org.jboss.logging.Logger;

/**
 * A logging wrapper to be used for {@link GridDialect}s which support the execution of native queries.
 *
 * @author Sebastien Lorber (<i>lorber.sebastien@gmail.com</i>)
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 *
 * @see GridDialectInitiator
 */
public class QueryableGridDialectLogger extends GridDialectLogger implements QueryableGridDialect {

	private static final Log log = Logger.getMessageLogger( Log.class, CoreLogCategories.DATASTORE_ACCESS.toString() );

	private final QueryableGridDialect gridDialect; // the real wrapped grid dialect

	public QueryableGridDialectLogger(QueryableGridDialect gridDialect) {
		super( gridDialect );
		this.gridDialect = gridDialect;
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery query, QueryParameters queryParameters) {
		log.tracef( "Executing backend query: %1$s", query.getQuery() );
		return gridDialect.executeBackendQuery( query, queryParameters );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return gridDialect.getParameterMetadataBuilder();
	}

	@Override
	public Object parseNativeQuery(String nativeQuery) {
		return gridDialect.parseNativeQuery( nativeQuery );
	}
}
