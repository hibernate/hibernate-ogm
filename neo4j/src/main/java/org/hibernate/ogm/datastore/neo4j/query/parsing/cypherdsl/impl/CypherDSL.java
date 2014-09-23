/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.ogm.util.impl.Contracts;

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
	private static final Pattern SIMPLE_NAME = Pattern.compile( "\\p{Alpha}[\\p{Alpha}\\d]*" );

	public static void identifier(StringBuilder builder, String identifier) {
		escapeIdentifier( builder, identifier );
	}

	public static StringBuilder identifier(StringBuilder builder, String identifier, String propertyName) {
		identifier( builder, identifier );
		if ( propertyName != null ) {
			builder.append( "." );
			escapeIdentifier( builder, propertyName );
		}
		return builder;
	}

	public static void as(StringBuilder builder, String alias) {
		if ( alias != null ) {
			builder.append( " as " );
			escapeIdentifier( builder, alias );
		}
	}

	public static StringBuilder compare(StringBuilder builder, String operator, Object value) {
		Contracts.assertNotNull( value, "Value" );
		builder.append( operator );
		literal( builder, value );
		return builder;
	}

	public static StringBuilder node(StringBuilder builder, String alias, String... labels) {
		builder.append( "(" );
		escapeIdentifier( builder, alias );
		for ( String label : labels ) {
			builder.append( ":" );
			escapeIdentifier( builder, label );
		}
		builder.append( ")" );
		return builder;
	}

	public static void collection(StringBuilder builder, List<Object> values) {
		builder.append( "[" );
		int counter = 1;
		for ( Object value : values ) {
			literal( builder, value );
			if ( counter++ < values.size() ) {
				builder.append( ", " );
			}
		}
		builder.append( "]" );
	}

	public static void limit(StringBuilder builder, Integer maxRows) {
		builder.append( " LIMIT " ).append( maxRows );
	}

	public static void skip(StringBuilder builder, Integer firstRow) {
		builder.append( " SKIP " ).append( firstRow );
	}

	public static void literal(StringBuilder builder, Object value) {
		if ( value instanceof String ) {
			builder.append( '"' );
			escapeLiteral( builder, value );
			builder.append( '"' );
		}
		else {
			builder.append( value );
		}
	}

	public static void escapeIdentifier(StringBuilder builder, String name) {
		if ( SIMPLE_NAME.matcher( name ).matches() ) {
			builder.append( name );
		}
		else {
			builder.append( '`' ).append( name ).append( '`' );
		}
	}

	private static void escapeLiteral(StringBuilder builder, Object value) {
		builder.append( value.toString().replace( "\\", "\\\\" ).replace( "\"", "\\\"" ) );
	}

}
