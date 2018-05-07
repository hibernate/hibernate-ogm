/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;

/**
 * Infinispan remote-based implementation of {@link LikePredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteLikePredicate extends LikePredicate<InfinispanRemoteQueryBuilder> implements NegatablePredicate<InfinispanRemoteQueryBuilder> {

	public InfinispanRemoteLikePredicate(String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );
	}

	@Override
	public InfinispanRemoteQueryBuilder getNegatedQuery() {
		return new InfinispanRemoteQueryBuilder( "not ", getQuery() );
	}

	@Override
	public InfinispanRemoteQueryBuilder getQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder();

		builder.append( propertyName );
		builder.append( " LIKE " );
		builder.appendValue( patternValue );

		return builder;
	}
}
