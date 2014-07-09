/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl;

import static org.neo4j.cypherdsl.CypherQuery.identifier;

import org.neo4j.cypherdsl.query.AbstractExpression;

/**
 * Represents matching a label to a value
 *
 * @deprecated Update {@code org.neo4j:neo4j-cypher-dsl} to version 2.0.2 and use the methods in {@code CypherQuery}
 * instead
 */
@Deprecated
public class LabelValue extends AbstractExpression {

	private final String[] labels;
	private final String identifier;

	public LabelValue(String identifier, String... labels) {
		this.identifier = identifier;
		this.labels = labels;
	}

	@Override
	public void asString(StringBuilder builder) {
		identifier( identifier ).asString( builder );
		for ( String label : labels ) {
			builder.append( ":" );
			builder.append( label );
		}
	}

}
