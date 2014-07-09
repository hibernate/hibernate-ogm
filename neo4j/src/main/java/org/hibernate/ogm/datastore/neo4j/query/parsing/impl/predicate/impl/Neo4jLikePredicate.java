/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.has;
import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.not;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;
import org.neo4j.cypherdsl.Property;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jLikePredicate extends LikePredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	private final String regexp;
	private final String alias;

	public Neo4jLikePredicate(String alias, String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );
		this.alias = alias;

		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( escapeCharacter );
		regexp = converter.getRegExpFromLikeExpression( patternValue ).pattern();
	}

	@Override
	public BooleanExpression getQuery() {
		return property().regexp( regexp );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
		return not( has( property() ) ).or( not( getQuery() ) );
	}

	private Property property() {
		return identifier( alias ).property( propertyName );
	}

}
