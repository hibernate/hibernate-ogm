/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.queryable.spi.BackendQuery;
import org.hibernate.ogm.dialect.queryable.spi.ClosableIterator;
import org.hibernate.ogm.dialect.queryable.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.queryable.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Batch delegator to be used for {@link GridDialect}s which support the execution of native queries.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class QueryableBatchOperationsDelegator<T extends Serializable> extends BatchOperationsDelegator implements QueryableGridDialect<T> {

	private final QueryableGridDialect<T> dialect;

	@SuppressWarnings("unchecked")
	public QueryableBatchOperationsDelegator(BatchableGridDialect dialect) {
		super( dialect );
		this.dialect = (QueryableGridDialect<T>) dialect;
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<T> query, QueryParameters queryParameters) {
		return dialect.executeBackendQuery( query, queryParameters );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return dialect.getParameterMetadataBuilder();
	}

	@Override
	public T parseNativeQuery(String nativeQuery) {
		return dialect.parseNativeQuery( nativeQuery );
	}
}
