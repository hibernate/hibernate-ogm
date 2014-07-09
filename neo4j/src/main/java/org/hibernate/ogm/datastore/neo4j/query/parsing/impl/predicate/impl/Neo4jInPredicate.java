/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.any;
import static org.neo4j.cypherdsl.CypherQuery.collection;
import static org.neo4j.cypherdsl.CypherQuery.has;
import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.none;
import static org.neo4j.cypherdsl.CypherQuery.not;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.neo4j.cypherdsl.Property;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jInPredicate extends InPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	private static final String X = "_x_";
	private final String alias;

	public Neo4jInPredicate(String alias, String columnName, List<Object> values) {
		super( columnName, values );
		this.alias = alias;
	}

	/**
	 * ANY( x IN values WHERE n.propertyName = x)
	 */
	@Override
	public BooleanExpression getQuery() {
		return any( X, collection( values() ), property().eq( identifier( X ) ) );
	}

	/**
	 * NOT HAS n.propertyName OR NONE( x IN values WHERE n.propertyName = x)
	 */
	@Override
	public BooleanExpression getNegatedQuery() {
		return not( has( property() ) ).or( none( X, collection( values() ), property().eq( identifier( X ) ) ) );
	}

	private Property property() {
		return identifier( alias ).property( propertyName );
	}

	private Object[] values() {
		return values.toArray( new Object[values.size()] );
	}

}
