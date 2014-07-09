/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escape;

/**
 * Example: (n:Example1:Example2)
 *
 * @author Davide D'Alto
 */
public class NodeExpression implements CypherExpression {

	private final String alias;
	private final String[] labels;

	public NodeExpression(String alias, String[] labels) {
		this.alias = alias;
		this.labels = labels;
	}

	@Override
	public void asString(StringBuilder builder) {
		builder.append( "(" );
		escape( builder, alias );
		for ( String label : labels ) {
			builder.append( ":" );
			escape( builder, label );
		}
		builder.append( ")" );
	}

}
