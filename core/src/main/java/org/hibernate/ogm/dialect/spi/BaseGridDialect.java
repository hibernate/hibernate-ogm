/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.BackendQuery;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.type.Type;

/**
 * Recommended base class for {@link GridDialect} implementations.
 *
 * @author Gunnar Morling
 */
public abstract class BaseGridDialect implements GridDialect {

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery backendQuery, QueryParameters queryParameters) {
		throw new UnsupportedOperationException( "Execution of native queries is not supported by this dialect" );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}

	@Override
	public Object parseNativeQuery(String nativeQuery) {
		throw new UnsupportedOperationException( "Execution of native queries is not supported by this dialect" );
	}
}
