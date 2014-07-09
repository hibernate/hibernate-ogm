/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.collection;

import java.util.List;

/**
 * Example:
 *
 * ANY( _x_ IN values WHERE n.propertyName = _x_)
 *
 * @author Davide D'Alto
 */
public class InExpression implements CypherExpression {

	private static final String X = "_x_";

	private final IdentifierExpression identifier;
	private final List<Object> values;

	public InExpression(IdentifierExpression identifier, List<Object> values) {
		this.identifier = identifier;
		this.values = values;
	}

	@Override
	public void asString(StringBuilder builder) {
		builder.append( "ANY( " );
		builder.append( X );
		builder.append( " IN " );
		collection( builder, values );
		builder.append( " WHERE " );
		identifier.asString( builder );
		builder.append( " = " );
		builder.append( X );
		builder.append( ")" );
	}

}
