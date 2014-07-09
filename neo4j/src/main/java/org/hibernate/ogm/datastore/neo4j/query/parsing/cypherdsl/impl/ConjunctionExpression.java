/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * @author Davide D'Alto
 */
public class ConjunctionExpression implements CypherExpression {

	public enum Type {
		OR, AND
	}

	private final Type type;
	private final CypherExpression[] booleanExpressions;

	public ConjunctionExpression(Type type, CypherExpression... booleanExpressions) {
		this.type = type;
		this.booleanExpressions = booleanExpressions;
	}

	@Override
	public void asString(StringBuilder builder) {
		if ( booleanExpressions != null && booleanExpressions.length > 0 ) {
			int counter = 1;
			for ( CypherExpression booleanExpression : booleanExpressions ) {
				builder.append( " (" );
				booleanExpression.asString( builder );
				builder.append( ") " );
				if ( counter < booleanExpressions.length ) {
					builder.append( type.name() );
				}
				counter++;
			}
		}
	}

}
