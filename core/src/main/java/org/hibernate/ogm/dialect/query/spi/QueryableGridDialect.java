/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import java.io.Serializable;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A facet for {@link GridDialect} implementations which support the execution of native queries.
 *
 * @author Gunnar Morling
 * @author Thorsten Möller
 *
 * @param <T> The type of native queries supported by this dialect
 */
public interface QueryableGridDialect<T extends Serializable> extends GridDialect {

	/**
	 * Returns the result of a native query executed on the backend.
	 *
	 * @param query the query to execute in a representation understood by the underlying datastore. May have been
	 * created by converting a JP-QL query or from a (named) native query.
	 * @param queryParameters parameters passed for this query
	 * @param tupleContext the tuple context
	 * @return an {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> executeBackendQuery(BackendQuery<T> query, QueryParameters queryParameters, TupleContext tupleContext);

	/**
	 * Returns the result of a native update query executed on the backend.
	 * <p>
	 * Precise semantics of an <i>update</i> are subject to design choices made
	 * for the actual database system in the backend. In particular, it may implement
	 * non-classical notions of consistency such as <i>eventual consistency</i>
	 * (rather than (conflict) serializablity as in most relational SQL database
	 * systems).
	 *
	 * @param query the query to execute in a representation understood by the underlying datastore. May have been
	 * created by converting a JP-QL query or from a (named) native query.
	 * @param queryParameters parameters passed for this query
	 * @param tupleContext the tuple context
	 * @return the number of elements that have been updated.
	 */
	int executeBackendUpdateQuery(BackendQuery<T> query, QueryParameters queryParameters, TupleContext tupleContext);

	/**
	 * Returns a builder for retrieving parameter meta-data from native queries in this datastore's format.
	 *
	 * @return a builder for retrieving parameter meta-data
	 */
	ParameterMetadataBuilder getParameterMetadataBuilder();

	/**
	 * Parses the given native query into a representation executable by this dialect.
	 *
	 * @param nativeQuery the native query to parse
	 * @return the parsed query
	 */
	T parseNativeQuery(String nativeQuery);
}
