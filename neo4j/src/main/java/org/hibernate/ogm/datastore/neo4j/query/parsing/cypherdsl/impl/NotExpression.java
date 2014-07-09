/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * Example: NOT (expression)
 *
 * @author Davide D'Alto
 */
public class NotExpression implements CypherExpression {

	private final CypherExpression booleanExpression;

	public NotExpression(CypherExpression booleanExpression) {
		this.booleanExpression = booleanExpression;
	}

	@Override
	public void asString(StringBuilder builder) {
		builder.append( " NOT (" );
		booleanExpression.asString( builder );
		builder.append( ") " );
	}

}
