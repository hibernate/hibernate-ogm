/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import static org.hibernate.ogm.util.impl.TupleContextHelper.tupleContext;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.loader.nativeloader.impl.BackendCustomQuery;
import org.hibernate.ogm.type.spi.TypeTranslator;

/**
 * An execution plan for a native NoSQL query. This custom implementation will allow us to implement native update
 * queries.
 *
 * @author Gunnar Morling
 * @author Thorsten Möller
 */
class NativeNoSqlQueryPlan extends NativeSQLQueryPlan {

	public NativeNoSqlQueryPlan(String sourceQuery, CustomQuery customQuery) {
		super( sourceQuery, customQuery );
	}

	@Override
	public int performExecuteUpdate(org.hibernate.engine.spi.QueryParameters queryParameters, SharedSessionContractImplementor session) throws HibernateException {
		QueryableGridDialect<?> gridDialect = session.getFactory().getServiceRegistry().getService( QueryableGridDialect.class );
		TypeTranslator typeTranslator = session.getFactory().getServiceRegistry().getService( TypeTranslator.class );
		return performExecuteUpdateQuery( gridDialect, QueryParameters.fromOrmQueryParameters( queryParameters, typeTranslator, session.getFactory() ), session );
	}

	@SuppressWarnings("unchecked")
	private <T extends Serializable> int performExecuteUpdateQuery(QueryableGridDialect<T> gridDialect, QueryParameters queryParameters,
			SharedSessionContractImplementor session) {
		// Safe cast, see
		// org.hibernate.ogm.query.impl.NativeNoSqlQueryInterpreter.createQueryPlan(NativeSQLQuerySpecification,
		// SessionFactoryImplementor)
		BackendCustomQuery<T> customQuery = (BackendCustomQuery<T>) getCustomQuery();
		BackendQuery<T> backendQuery = new BackendQuery<T>( customQuery.getQueryObject(), customQuery.getSingleEntityMetadataInformationOrNull() );
		TupleContext tupleContext = tupleContext( session, customQuery.getSingleEntityMetadataInformationOrNull() );
		return gridDialect.executeBackendUpdateQuery( backendQuery, queryParameters, tupleContext );
	}
}
