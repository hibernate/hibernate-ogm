/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.nativeloader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.spi.NativeNoSqlQuerySpecification;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Extension point allowing any NoSQL native query with named and positional parameters
 * to be executed by OGM on the corresponding backend, returning managed entities, collections and
 * simple scalar values.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BackendCustomQuery implements CustomQuery {

	private static final Log LOG = LoggerFactory.make();

	private final String queryString;
	private final Object queryObject;
	private final SessionFactoryImplementor sessionFactory;
	private final Set<String> querySpaces;
	private final List<Return> customQueryReturns;

	public BackendCustomQuery(NativeNoSqlQuerySpecification spec, SessionFactoryImplementor factory) throws HibernateException {
		LOG.tracev( "Starting processing of NoSQL query [{0}]", spec.getQueryString() );

		this.queryString = spec.getQueryString();
		this.queryObject = spec.getQueryObject();
		this.sessionFactory = factory;

		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(spec.getQueryReturns(), factory);
		processor.process();
		customQueryReturns = Collections.unmodifiableList( processor.generateCustomReturns( false ) );

		if ( spec.getQuerySpaces() != null ) {
			@SuppressWarnings("unchecked")
			Set<String> spaces = spec.getQuerySpaces();
			querySpaces = Collections.<String>unmodifiableSet( spaces );
		}
		else {
			querySpaces = Collections.emptySet();
		}
	}

	/**
	 * @deprecated Use {@link #getQueryString()} instead.
	 */
	@Override
	@Deprecated
	public String getSQL() {
		return getQueryString();
	}

	public String getQueryString() {
		return queryString;
	}

	/**
	 * Returns an object-based representation of this query, if present.
	 *
	 * @return an object-based representation of this query, or {@code null} if this is a string-based query.
	 */
	public Object getQueryObject() {
		return queryObject;
	}

	@Override
	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public Map<?, ?> getNamedParameterBindPoints() {
		// TODO: Should this actually be something more sensible?
		return Collections.emptyMap();
	}

	@Override
	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	/**
	 * Returns the {@link EntityKeyMetadata} of the entity type selected by this query.
	 *
	 * @return the {@link EntityKeyMetadata} of the entity type selected by this query or {@code null} in case this
	 * query does not select exactly one entity type (e.g. in case of scalar values or joins (if supported in future revisions)).
	 */
	public EntityKeyMetadata getSingleEntityKeyMetadataOrNull() {
		EntityKeyMetadata metadata = null;

		for ( Return queryReturn : getCustomQueryReturns() ) {
			if ( queryReturn instanceof RootReturn ) {
				if ( metadata != null ) {
					return null;
				}
				RootReturn rootReturn = (RootReturn) queryReturn;
				OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getEntityPersister( rootReturn.getEntityName() );
				metadata = new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
			}
		}

		return metadata;
	}

	@Override
	public String toString() {
		return queryObject != null ? queryObject.toString() : queryString;
	}
}
