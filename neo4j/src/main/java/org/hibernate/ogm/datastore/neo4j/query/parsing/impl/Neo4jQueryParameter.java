/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;


/**
 * @author Gunnar Morling
 *
 */
public class Neo4jQueryParameter {

	private final String name;

	public Neo4jQueryParameter(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "{" + name + "}";
	}
}
