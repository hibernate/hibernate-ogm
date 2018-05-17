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
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;

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

	/**
	 * Constructor for conjunction or disjunction predicate
	 *
	 * @param operator should be "and" or "or"
	 * @param negateChildren indicates if apply a negation of each sub predicates
	 * @param children sub predicates
	 */
	public InfinispanRemoteQueryBuilder(String operator, boolean negateChildren, List<Predicate<InfinispanRemoteQueryBuilder>> children) {
		this.builder = new StringBuilder( "(" );

		int counter = 1;
		for ( Predicate<InfinispanRemoteQueryBuilder> child : children ) {
			InfinispanRemoteQueryBuilder nestedQuery = ( negateChildren ) ?
					( (NegatablePredicate<InfinispanRemoteQueryBuilder>) child ).getNegatedQuery()
					: child.getQuery();

			builder.append( nestedQuery );
			builder.append( ")" );
			if ( counter++ < children.size() ) {
				builder.append( " " );
				builder.append( operator );
				builder.append( " (" );
			}
		}
	}

	public void append(String content) {
		builder.append( content );
	}

	public void append(InfinispanRemoteQueryBuilder child) {
		builder.append( child.builder );
	}

	public void appendValue(Object value) {
		boolean isText = value instanceof String;

		if ( isText ) {
			builder.append( "'" );
		}
		builder.append( value );
		if ( isText ) {
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
			throw new HibernateException( "Impossible to add a where clause twice in the same query" );
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
