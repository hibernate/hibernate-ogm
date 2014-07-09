/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.ConjunctionExpression.Type;

/**
 * Contains a set of function that can be used to create a cypher query.
 *
 * @author Davide D'Alto
 */
public class CypherDSL {

	/*
	 * From Neo4j manual v2.1.2:
	 *
	 * Identifier names are case sensitive, and can contain underscores and alphanumeric
	 * characters (a-z, 0-9), but must always start with a letter. If other characters are needed, you can quote the
	 * identifier using backquote (`) signs. The same rules apply to property names.
	 */
	private static final Pattern SIMPLE_NAME = Pattern.compile( "\\p{Alpha}\\w*" );

	public static void escape(StringBuilder builder, String name) {
		if ( SIMPLE_NAME.matcher( name ).matches() ) {
			builder.append( name );
		}
		else {
			builder.append( '`' );
			builder.append( name );
			builder.append( '`' );
		}
	}

	public static IdentifierExpression identifier(String alias) {
		return new IdentifierExpression( alias );
	}

	public static NodeExpression node(String alias, String... labels) {
		return new NodeExpression(alias, labels);
	}

	public static WhereClause match(NodeExpression node) {
		StringBuilder builder = new StringBuilder( "MATCH " );
		node.asString( builder );
		return new WhereClause( builder );
	}

	public static CypherExpression not(CypherExpression expression) {
		return new NotExpression( expression );
	}

	public static CypherExpression or(CypherExpression... expressions) {
		return new ConjunctionExpression( Type.OR, expressions );
	}

	public static CypherExpression and(CypherExpression... expressions) {
		return new ConjunctionExpression( Type.AND, expressions );
	}

	public static CypherExpression has(IdentifierExpression identifier) {
		return new HasExpression( identifier );
	}

	public static void collection(StringBuilder builder, List<Object> values) {
		builder.append( "[" );
		int counter = 1;
		for ( Object value : values ) {
			literal( value ).asString( builder );
			if ( counter++ < values.size() ) {
				builder.append( ", " );
			}
		}
		builder.append( "]" );
	}

	public static void limit(StringBuilder builder, Integer maxRows) {
		builder.append( " LIMIT " );
		builder.append( maxRows );
	}

	public static void skip(StringBuilder builder, Integer firstRow) {
		builder.append( " SKIP " );
		builder.append( firstRow );
	}

	public static LiteralExpression literal(Object value) {
		return new LiteralExpression( value );
	}
}
