/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;

/**
 * Interprets given native NoSQL queries.
 *
 * @author Gunnar Morling
 *
 */
public class NativeNoSqlQueryInterpreter implements NativeQueryInterpreter {

	private final ParameterMetadataBuilder builder;

	public NativeNoSqlQueryInterpreter(ParameterMetadataBuilder builder) {
		this.builder = builder;
	}

	@Override
	public ParameterMetadata getParameterMetadata(String nativeQuery) {
		return builder.buildParameterMetadata( nativeQuery );
	}

	@Override
	public NativeSQLQueryPlan createQueryPlan(NativeSQLQuerySpecification specification, SessionFactoryImplementor sessionFactory) {
		return new NativeNoSqlQueryPlan(
				specification.getQueryString(),
				new BackendCustomQuery( specification, sessionFactory )
		);
	}
}
