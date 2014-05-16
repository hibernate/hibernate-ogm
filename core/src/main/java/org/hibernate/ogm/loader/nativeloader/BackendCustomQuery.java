/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.nativeloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Extension point allowing any NoSQL native query with named and positional parameters
 * to be executed by OGM on the corresponding backend, returning managed entities, collections and
 * simple scalar values.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BackendCustomQuery implements CustomQuery {

	private static final Log LOG = LoggerFactory.make();

	private final String query;
	private final Set<Object> querySpaces = new HashSet<Object>();
	private final Map<Object, Object> namedParameterBindPoints = new HashMap<Object, Object>();
	private final List<Object> customQueryReturns = new ArrayList<Object>();

	public BackendCustomQuery(final String nosqlQuery, final NativeSQLQueryReturn[] queryReturns, final Collection<?> additionalQuerySpaces,
			final SessionFactoryImplementor factory) throws HibernateException {

		LOG.tracev( "Starting processing of NoSQL query [{0}]", nosqlQuery );

		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(queryReturns, factory);
		processor.process();
		Collection<?> customReturns = processor.generateCustomReturns( false );
		customQueryReturns.addAll( customReturns );

		this.query = nosqlQuery;

		if ( additionalQuerySpaces != null ) {
			querySpaces.addAll( additionalQuerySpaces );
		}

	}

	@Override
	public String getSQL() {
		return query;
	}

	@Override
	public Set getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public Map getNamedParameterBindPoints() {
		return namedParameterBindPoints;
	}

	@Override
	public List getCustomQueryReturns() {
		return customQueryReturns;
	}

}
