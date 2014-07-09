/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * Compare an identifier with a value using the defined operator
 *
 * @author Davide D'Alto
 */
public class ComparisonExpression implements CypherExpression {

	private final IdentifierExpression identifier;
	private final String operator;
	private final CypherExpression value;

	public ComparisonExpression(IdentifierExpression identifier, String operator, CypherExpression value) {
		this.identifier = identifier;
		this.operator = operator;
		this.value = value;
	}

	@Override
	public void asString(StringBuilder builder) {
		identifier.asString( builder );
		builder.append( operator );
		value.asString( builder );
	}

}
