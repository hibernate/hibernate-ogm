/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;

/**
 * Infinispan remote-based implementation of {@link NegationPredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteNegationPredicate extends NegationPredicate<InfinispanRemoteQueryBuilder> implements NegatablePredicate<InfinispanRemoteQueryBuilder> {

	@Override
	public InfinispanRemoteQueryBuilder getNegatedQuery() {
		return getChild().getQuery();
	}

	@Override
	public InfinispanRemoteQueryBuilder getQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder();
		if ( getChild() instanceof NegatablePredicate ) {
			NegatablePredicate<InfinispanRemoteQueryBuilder> negatable = (NegatablePredicate<InfinispanRemoteQueryBuilder>) getChild();
			return negatable.getNegatedQuery();
		}
		else {
			builder.append( "NOT (" );
			getChild().getQuery();
			builder.append( ")" );
			return builder;
		}
	}
}
