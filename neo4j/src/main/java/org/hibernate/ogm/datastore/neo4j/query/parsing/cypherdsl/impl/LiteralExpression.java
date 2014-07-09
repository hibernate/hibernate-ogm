/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

/**
 * if the value is a string, surround it with double quotes (") characters otherwise, otherwise append it to the builder
 * as it is.
 * <p>
 * The value is goint to be escaped when required
 *
 * @author Davide D'Alto
 */
public class LiteralExpression implements CypherExpression {

	private final Object value;

	public LiteralExpression(Object value) {
		this.value = value;
	}

	@Override
	public void asString(StringBuilder builder) {
		if ( value instanceof String ) {
			builder.append( '"' );
			builder.append( escape( value ) );
			builder.append( '"' );
		}
		else {
			builder.append( value );
		}
	}

	private String escape(Object value) {
		return value.toString().replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
	}

}
