/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.impl;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Describes a query to be executed against Infinispan Server.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryDescriptor implements Serializable {

	private final String cache;
	private final String query;
	private final String[] projections;

	public InfinispanRemoteQueryDescriptor(String cache, String query, String[] projections) {
		this.cache = cache;
		this.query = query;
		this.projections = projections;
	}

	public String getCache() {
		return cache;
	}

	public String getQuery() {
		return query;
	}

	public String[] getProjections() {
		return projections;
	}

	@Override
	public String toString() {
		return "InfinispanRemoteQueryDescriptor{" +
				"cache='" + cache + '\'' +
				", query='" + query + '\'' +
				", projections='" + Arrays.toString( projections ) + '\'' +
				'}';
	}
}
