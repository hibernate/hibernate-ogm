/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.and;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.or;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.literal;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.ComparisonExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.IdentifierExpression;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jRangePredicate extends RangePredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	private final IdentifierExpression identifier;

	public Neo4jRangePredicate(String alias, String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
		identifier = identifier( alias ).property( propertyName );
	}

	@Override
	public CypherExpression getQuery() {
		return and( comparator( identifier, ">=", lower ), comparator( identifier, "<=", upper ) );
	}

	@Override
	public CypherExpression getNegatedQuery() {
		return or( comparator( identifier, "<", lower ), comparator( identifier, ">", upper ) );
	}

	private ComparisonExpression comparator(IdentifierExpression identifier, String operator, Object value) {
		Contracts.assertNotNull( value, "Value" );
		return new ComparisonExpression( identifier, operator, literal( value ) );
	}

}
