/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.compare;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.util.parser.impl.LikeExpressionToRegExpConverter;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jLikePredicate extends LikePredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final String regexp;
	private final StringBuilder builder;
	private final String alias;

	public Neo4jLikePredicate(StringBuilder builder, String alias, String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );
		this.builder = builder;
		this.alias = alias;

		LikeExpressionToRegExpConverter converter = new LikeExpressionToRegExpConverter( escapeCharacter );
		regexp = converter.getRegExpFromLikeExpression( patternValue ).pattern();
	}

	/**
	 * <pre>{@code n.property =~ '...'}</pre>
	 */
	@Override
	public StringBuilder getQuery() {
		return compare( identifier( builder, alias, propertyName ), "=~", regexp );
	}

	/**
	 * <pre>
	 * {@code NOT (HAS aslias.property) OR ( NOT ( alias.property =~ '...' )}
	 * </pre>
	 */
	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( "NOT HAS(" );
		identifier( builder, alias, propertyName );
		builder.append( ") OR NOT(" );
		getQuery();
		builder.append( ")" );
		return builder;
	}
}
