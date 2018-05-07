/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;

/**
 * Helper class to append single or multiple values to the wrapped {@link StringBuilder}
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteQueryBuilder implements Serializable {

	private StringBuilder builder;
	private boolean where;

	public InfinispanRemoteQueryBuilder() {
		this.builder = new StringBuilder();
	}

	public InfinispanRemoteQueryBuilder(String content) {
		this.builder = new StringBuilder( content );
	}

	public InfinispanRemoteQueryBuilder(String content, String otherContent) {
		this.builder = new StringBuilder( content );
		append( otherContent );
	}

	public InfinispanRemoteQueryBuilder(String content, InfinispanRemoteQueryBuilder child) {
		this.builder = new StringBuilder( content );
		append( child );
	}

	public void append(String content) {
		builder.append( content );
	}

	public void append(InfinispanRemoteQueryBuilder child) {
		builder.append( child.builder );
	}

	public void appendValue(Object value) {
		boolean isConstant = value instanceof String;

		if ( isConstant ) {
			builder.append( "'" );
		}
		builder.append( value );
		if ( isConstant ) {
			builder.append( "'" );
		}
	}

	public void appendValues(List<?> values) {
		for ( int i = 0; i < values.size(); i++ ) {
			appendValue( values.get( i ) );

			if ( i != values.size() - 1 ) {
				builder.append( ", " );
			}
		}
	}

	public void appendStrings(List<String> values) {
		for ( int i = 0; i < values.size(); i++ ) {
			builder.append( values.get( i ) );

			if ( i != values.size() - 1 ) {
				builder.append( ", " );
			}
		}
	}

	public void addWhere(InfinispanRemoteQueryBuilder subQuery) {
		if ( where ) {
			throw new HibernateException( "Impossible to add two times a where clause to the same query" );
		}

		builder.append( " where " );
		builder.append( subQuery );
		where = true;
	}

	public String getQuery() {
		return builder.toString();
	}

	public boolean hasWhere() {
		return where;
	}

	@Override
	public String toString() {
		return builder.toString();
	}
}
