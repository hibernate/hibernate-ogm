/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi;

import javax.persistence.NamedNativeQuery;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.EntityKeyMetadata;

/**
 * Represents a NoSQL query as to be executed via
 * {@link GridDialect#executeBackendQuery(NoSqlQuery, org.hibernate.engine.spi.QueryParameters)}.
 * <p>
 * The wrapped query object generally represents the query in the native form supported by a given datastore, e.g. a
 * String in a native query syntax. Dialects may be able to deal with several representations, e.g. the MongoDB dialect
 * can deal with Strings in the CLI query syntax as well as queries in form of {@code DBObject}s. Depending on the
 * origin of this query object (e.g. from a named native query specified via {@link NamedNativeQuery} or translated from
 * a JP-QL query, one form or the other may be actually used.
 *
 * @author Gunnar Morling
 */
public class BackendQuery {

	private final Object query;
	private final EntityKeyMetadata singleEntityKeyMetadata;

	public BackendQuery(Object query, EntityKeyMetadata singleEntityKeyMetadata) {
		this.query = query;
		this.singleEntityKeyMetadata = singleEntityKeyMetadata;
	}

	public Object getQuery() {
		return query;
	}

	public EntityKeyMetadata getSingleEntityKeyMetadataOrNull() {
		return singleEntityKeyMetadata;
	}
}
