/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jIsNullPredicate extends IsNullPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;
	private final String alias;

	public Neo4jIsNullPredicate(StringBuilder builder, String alias, String propertyName) {
		super( propertyName );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		builder.append( "NOT HAS(" );
		identifier( builder, alias, propertyName );
		builder.append( ")" );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( "HAS(" );
		identifier( builder, alias, propertyName );
		builder.append( ")" );
		return builder;
	}

}
