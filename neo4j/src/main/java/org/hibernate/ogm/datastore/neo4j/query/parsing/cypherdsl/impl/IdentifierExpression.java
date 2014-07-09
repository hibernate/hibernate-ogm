/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escape;

/**
 * Represents an identifier in the form: identifier.property as alias
 * <p>
 * The name of the identifier, property o alias is going to be escaped if requires.
 *
 * @author Davide D'Alto
 */
public class IdentifierExpression implements CypherExpression {

	private final String identifier;
	private String property;
	private String alias;

	public IdentifierExpression(String alias) {
		this.identifier = alias;
	}

	public IdentifierExpression property(String property) {
		this.property = property;
		return this;
	}

	public IdentifierExpression as(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public void asString(StringBuilder builder) {
		escape( builder, identifier );
		if ( property != null ) {
			builder.append( "." );
			escape( builder, property );
		}
		if ( alias != null ) {
			builder.append( " as " );
			escape( builder, alias );
		}
	}

}
