/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * Represents a part of a cypher query.
 *
 * @author Davide D'Alto
 */
public interface CypherExpression {

	/**
	 * Appends to the {@link StringBuilder} the corresponding part for the cypher query.
	 *
	 * @param builder contains the cypher query
	 */
	void asString(StringBuilder builder);

}
