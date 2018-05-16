/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.util.List;

import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;
import org.hibernate.ogm.query.spi.QueryParsingResult;

/**
 * The result of walking a query parse tree using a {@link InfinispanRemoteQueryRendererDelegate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryParsingResult implements QueryParsingResult {

	private final String query;
	private final String cache;
	private final List<String> projection;

	public InfinispanRemoteQueryParsingResult(InfinispanRemoteQueryBuilder builder, String cache, List<String> projection) {
		this.query = builder.getQuery();
		this.cache = cache;
		this.projection = projection;
	}

	@Override
	public Object getQueryObject() {
		return new InfinispanRemoteQueryDescriptor( cache, query, projection );
	}

	@Override
	public List<String> getColumnNames() {
		return projection;
	}

	@Override
	public String toString() {
		return "InfinispanRemoteQueryParsingResult{" +
				"query='" + query + '\'' +
				", cache='" + cache + '\'' +
				", projection=" + projection +
				'}';
	}
}
