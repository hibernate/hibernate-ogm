/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.RootPredicate;

/**
 * @author Victor Kadachigov
 */
class IgniteRootPredicate extends RootPredicate<StringBuilder> {

	@Override
	public StringBuilder getQuery() {
		if ( child == null ) {
			return null;
		}
		else {
			return child.getQuery();
		}
	}

}
