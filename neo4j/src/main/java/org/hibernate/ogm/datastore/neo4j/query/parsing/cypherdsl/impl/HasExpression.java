/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * Example:
 *
 * HAS( n.property )
 *
 * @author Davide D'Alto
 */
public class HasExpression implements CypherExpression {

	private final IdentifierExpression identifier;

	public HasExpression(IdentifierExpression identifier) {
		this.identifier = identifier;
	}

	@Override
	public void asString(StringBuilder builder) {
		builder.append( " HAS( " );
		identifier.asString( builder );
		builder.append( " )" );
	}

}
