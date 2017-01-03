/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import java.io.Serializable;
import java.util.List;

/**
 * Describes a Ignite SQL query
 *
 * @author Victor Kadachigov
 */
public class IgniteQueryDescriptor implements Serializable {

	private static final long serialVersionUID = 8197979441369153954L;

	private final String sql;
	private final List<Object> indexedParameters;
	private final boolean hasScalar;
//	private final List<Return> customQueryReturns;
//	private final Set<String> querySpaces;

	public IgniteQueryDescriptor(String sql, List<Object> indexedParameters, boolean hasScalar) {
		this.sql = sql;
		this.indexedParameters = indexedParameters;
		this.hasScalar = hasScalar;
	}

	public List<Object> getIndexedParameters() {
		return indexedParameters;
	}

	public String getSql() {
		return sql;
	}

	public boolean isHasScalar() {
		return hasScalar;
	}

}
