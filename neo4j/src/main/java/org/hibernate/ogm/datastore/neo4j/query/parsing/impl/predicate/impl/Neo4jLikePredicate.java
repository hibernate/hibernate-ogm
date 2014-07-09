/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.has;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.literal;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.not;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.or;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.ComparisonExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.IdentifierExpression;
import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jLikePredicate extends LikePredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	private final String regexp;
	private final IdentifierExpression identifier;

	public Neo4jLikePredicate(String alias, String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );
		identifier = identifier( alias ).property( propertyName );

		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( escapeCharacter );
		regexp = converter.getRegExpFromLikeExpression( patternValue ).pattern();
	}

	/**
	 * <pre>{@code n.property =~ '...'}</pre>
	 */
	@Override
	public CypherExpression getQuery() {
		return new ComparisonExpression( identifier, "=~", literal( regexp ) );
	}

	/**
	 * <pre>{@code NOT (HAS aslias.property) OR ( NOT ( alias.property =~ '...' )}</pre>
	 */
	@Override
	public CypherExpression getNegatedQuery() {
		return or( not( has( identifier ) ), not( getQuery() ) );
	}
}
