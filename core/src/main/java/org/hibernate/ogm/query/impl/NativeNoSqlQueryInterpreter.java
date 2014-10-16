/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.loader.nativeloader.impl.BackendCustomQuery;

/**
 * Interprets given native NoSQL queries.
 *
 * @author Gunnar Morling
 *
 */
public class NativeNoSqlQueryInterpreter implements NativeQueryInterpreter {

	private final QueryableGridDialect<?> gridDialect;
	private final ParameterMetadataBuilder builder;

	public NativeNoSqlQueryInterpreter(QueryableGridDialect<?> gridDialect) {
		this.gridDialect = gridDialect;
		this.builder = gridDialect.getParameterMetadataBuilder();
	}

	@Override
	public ParameterMetadata getParameterMetadata(String nativeQuery) {
		return builder.buildParameterMetadata( nativeQuery );
	}

	@Override
	public NativeSQLQueryPlan createQueryPlan(NativeSQLQuerySpecification specification, SessionFactoryImplementor sessionFactory) {
		CustomQuery customQuery = getCustomQuery( gridDialect, specification, sessionFactory );
		return new NativeNoSqlQueryPlan( specification.getQueryString(), customQuery );
	}

	private <T extends Serializable> CustomQuery getCustomQuery(QueryableGridDialect<T> gridDialect, NativeSQLQuerySpecification specification, SessionFactoryImplementor sessionFactory) {
		T query = gridDialect.parseNativeQuery( specification.getQueryString() );

		@SuppressWarnings("unchecked")
		Set<String> querySpaces = specification.getQuerySpaces();

		return new BackendCustomQuery<T>(
				specification.getQueryString(),
				query,
				specification.getQueryReturns(),
				querySpaces,
				sessionFactory
		);
	}
}
