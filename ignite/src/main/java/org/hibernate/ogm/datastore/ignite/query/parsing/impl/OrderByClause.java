/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl.PredicateHelper;

/**
 * Contains the details of the order required for a property
 *
 * @author Victor Kadachigov
 */
class OrderByClause {

	private final String alias;
	private final String propertyName;
	private final boolean isAscending;

	public OrderByClause(String identifier, String propertyName, boolean ascending) {
		this.alias = identifier;
		this.propertyName = propertyName;
		this.isAscending = ascending;
	}

	public void asString(StringBuilder builder) {
		PredicateHelper.identifier( builder, alias, propertyName );
		if ( !isAscending ) {
			builder.append( " DESC" );
		}
	}
}
