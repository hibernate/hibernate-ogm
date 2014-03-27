/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.parser.impl;

import java.util.regex.Pattern;

/**
 * Creates {@link Pattern} objects equivalent to given HQL/JPQL {@code LIKE} expressions.
 * <p>
 * Used by {@link org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBLikePredicate} to run {@code LIKE}
 * queries against MongoDB, using the {@code $regex} operator.
 * <p>
 * The following rules apply for creating regular expressions:
 * <ul>
 * <li>the {@code _} wildcard is replaced by {@code .} (unless it is escaped)</li>
 * <li>the {@code %} wildcard is replaced by {@code .*} (unless it is escaped)</li>
 * <li>non-wildcard character sequences are quoted (wrapped by {@code \\Q...\\E}) to match them as is</li>
 * <li>escape characters are omitted from the resulting pattern</li>
 * <li>the pattern is wrapped by {@code ^...$} to make sure the entire string is matched</li>
 * <li>the reg exp wildcard {@code .} matches line breaks</li>
 * </ul>
 *
 * @author Gunnar Morling
 */
public class LikeExpressionToRegExpConverter {

	private final Character escapeCharacter;

	public LikeExpressionToRegExpConverter() {
		this( null );
	}

	public LikeExpressionToRegExpConverter(Character escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	/**
	 * Creates a regular expression pattern object equivalent to the given {@code LIKE} expression.
	 *
	 * @param likeExpression the HQL/JPQL {@code LIKE} expression to convert
	 * @return a regular expression pattern object equivalent to the given {@code LIKE} expression
	 */
	public Pattern getRegExpFromLikeExpression(String likeExpression) {
		StringBuilder pattern = new StringBuilder( "^" );

		State state = State.INITIAL;
		for ( int i = 0; i < likeExpression.length(); i++ ) {
			char character = likeExpression.charAt( i );
			state = state.handleCharacter( character, escapeCharacter, pattern );
		}

		if ( state == State.PATTERN ) {
			pattern.append( "\\E" );
		}

		pattern.append( "$" );

		return Pattern.compile( pattern.toString(), Pattern.DOTALL );
	}

	/**
	 * Possible states while parsing a pattern.
	 */
	private enum State {

		INITIAL, ESCAPE, PATTERN, WILDCARD_CHARACTER;

		/**
		 * Handles the given character, e.g. by appending it to the result string, discarding it etc.
		 *
		 * @param character the character to process
		 * @param escapeCharacter the escape character, if any
		 * @param result the currently created regex pattern
		 * @return the next state as determined by processing the given character in the current state
		 */
		private State handleCharacter(Character character, Character escapeCharacter, StringBuilder result) {
			switch ( this ) {
				case INITIAL:
					if ( Character.valueOf( character ).equals( escapeCharacter ) ) {
						return State.ESCAPE;
					}
					else if ( character == '%' ) {
						result.append( ".*" );
						return State.WILDCARD_CHARACTER;
					}
					else if ( character == '_' ) {
						result.append( "." );
						return State.WILDCARD_CHARACTER;
					}
					else {
						result.append( "\\Q" );
						result.append( character );
						return State.PATTERN;
					}
				case PATTERN:
					if ( Character.valueOf( character ).equals( escapeCharacter ) ) {
						return State.ESCAPE;
					}
					else if ( character == '%' ) {
						result.append( "\\E" );
						result.append( ".*" );
						return State.WILDCARD_CHARACTER;
					}
					else if ( character == '_' ) {
						result.append( "\\E" );
						result.append( "." );
						return State.WILDCARD_CHARACTER;
					}
					else {
						result.append( character );
						return State.PATTERN;
					}
				case ESCAPE:
					result.append( character );
					return State.PATTERN;
				case WILDCARD_CHARACTER:
					if ( Character.valueOf( character ).equals( escapeCharacter ) ) {
						return State.ESCAPE;
					}
					else if ( character == '%' ) {
						result.append( ".*" );
						return State.WILDCARD_CHARACTER;
					}
					else if ( character == '_' ) {
						result.append( "." );
						return State.WILDCARD_CHARACTER;
					}
					else {
						result.append( "\\Q" );
						result.append( character );
						return State.PATTERN;
					}
				default:
					throw new IllegalStateException( "Unsupported parsing state" );
			}
		}
	}
}
