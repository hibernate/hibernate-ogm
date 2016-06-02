/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.query.spi;

import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.util.Experimental;

/**
 * Represents a NoSQL query as to be executed via
 * {@link QueryableGridDialect#executeBackendQuery(BackendQuery, QueryParameters, TupleContext)}
 * <p>
 * The wrapped query object generally represents the query in the native form supported by a given datastore, e.g. a
 * String in a native query syntax or an object-based query representation such as the {@code DBObject}-based query
 * representation in the case of MongoDB.
 *
 * @author Gunnar Morling
 * @param <T> The type of query as understood by the underlying dialect implementation
 */
@Experimental("This contract is under active development, incompatible changes may occur in future versions.")
public class BackendQuery<T> {

	private final T query;
	private final EntityMetadataInformation singleEntityMetadataInformation;

	public BackendQuery(T query, EntityMetadataInformation singleEntityMetadataInformation) {
		this.query = query;
		this.singleEntityMetadataInformation = singleEntityMetadataInformation;
	}

	public T getQuery() {
		return query;
	}

	public EntityMetadataInformation getSingleEntityMetadataInformationOrNull() {
		return singleEntityMetadataInformation;
	}

}
