/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * Contains the details of the order required for a property
 *
 * @author Davide D'Alto
 */
public class OrderByClause implements CypherExpression {

	private final IdentifierExpression identifier;
	private final boolean isAscending;

	public OrderByClause(IdentifierExpression identifier, boolean ascending) {
		this.identifier = identifier;
		this.isAscending = ascending;
	}

	public void asString(StringBuilder builder) {
		identifier.asString( builder );
		if ( !isAscending ) {
			builder.append( " DESC" );
		}
	}
}
