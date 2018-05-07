/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.io.Serializable;

/**
 * Class represents a parameter of an Infinispan server query
 */
public class InfinispanRemoteQueryParameter implements Serializable {

	private final String name;

	public InfinispanRemoteQueryParameter(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return ":" + name;
	}
}
