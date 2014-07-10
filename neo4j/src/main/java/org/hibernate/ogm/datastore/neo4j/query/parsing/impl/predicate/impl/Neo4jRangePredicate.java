/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.literal;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jRangePredicate extends RangePredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final String alias;
	private final StringBuilder builder;

	public Neo4jRangePredicate(StringBuilder builder, String alias, String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		comparator( ">=", lower );
		builder.append( " AND " );
		comparator( "<=", upper );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		comparator( "<", lower );
		builder.append( " OR " );
		comparator( ">", upper );
		return builder;
	}

	private void comparator(String operator, Object value) {
		identifier( builder, alias, propertyName );
		builder.append( operator );
		literal( builder, value );
	}

}
