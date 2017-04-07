/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

/**
 * @author Victor Kadachigov
 */
public class PredicateHelper {

	public static StringBuilder identifier(StringBuilder builder, String identifier, String propertyName) {
		builder.append( identifier );
		if ( propertyName != null ) {
			builder.append( "." );
			builder.append( propertyName );
		}
		return builder;
	}

	public static StringBuilder literal(StringBuilder builder, Object value) {
		if ( value instanceof String ) {
			builder.append( '\'' );
			builder.append( ( (String) value ).replace( "'", "''" ) );
			builder.append( '\'' );
		}
		else {
			builder.append( value );
		}
		return builder;
	}

}
