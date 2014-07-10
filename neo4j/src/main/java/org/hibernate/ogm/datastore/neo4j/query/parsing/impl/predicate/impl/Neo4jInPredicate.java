/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.collection;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jInPredicate extends InPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private static final String X = "_x_";

	private final StringBuilder builder;
	private final String alias;

	public Neo4jInPredicate(StringBuilder builder, String alias, String propertyName, List<Object> values) {
		super( propertyName, values );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		builder.append( "ANY( " );
		builder.append( X );
		builder.append( " IN " );
		collection( builder, values );
		builder.append( " WHERE " );
		identifier( builder, alias, propertyName );
		builder.append( " = " );
		builder.append( X );
		builder.append( ")" );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( "NOT HAS(" );
		identifier( builder, alias, propertyName );
		builder.append( ") OR " );
		builder.append( " NONE( " );
		builder.append( X );
		builder.append( " IN " );
		collection( builder, values );
		builder.append( " WHERE " );
		identifier( builder, alias, propertyName );
		builder.append( " = " );
		builder.append( X );
		builder.append( ")" );
		return builder;
	}

}
