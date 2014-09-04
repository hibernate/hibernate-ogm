/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.grid.spi.EntityKeyMetadata;

/**
 * Represents a NoSQL query as to be executed via
 * {@link GridDialect#executeBackendQuery(NoSqlQuery, org.hibernate.engine.spi.QueryParameters)}.
 * <p>
 * The wrapped query object generally represents the query in the native form supported by a given datastore, e.g. a
 * String in a native query syntax or an object-based query representation such as the {@code DBObject}-based query
 * representation in the case of MongoDB.
 *
 * @author Gunnar Morling
 * @param <T> The type of query as understood by the underlying dialect implementation
 */
public class BackendQuery<T> {

	private final T query;
	private final EntityKeyMetadata singleEntityKeyMetadata;

	public BackendQuery(T query, EntityKeyMetadata singleEntityKeyMetadata) {
		this.query = query;
		this.singleEntityKeyMetadata = singleEntityKeyMetadata;
	}

	public T getQuery() {
		return query;
	}

	public EntityKeyMetadata getSingleEntityKeyMetadataOrNull() {
		return singleEntityKeyMetadata;
	}
}
