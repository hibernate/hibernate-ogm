/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;

/**
 * Infinispan server-based implementation of {@link InPredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteInPredicate extends InPredicate<InfinispanRemoteQueryBuilder> implements NegatablePredicate<InfinispanRemoteQueryBuilder> {

	public InfinispanRemoteInPredicate(String propertyName, List<Object> values) {
		super( propertyName, values );
	}

	@Override
	public InfinispanRemoteQueryBuilder getQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder( propertyName );
		builder.append( " in (" );
		builder.appendValues( values );
		builder.append( ")" );

		return builder;
	}

	@Override
	public InfinispanRemoteQueryBuilder getNegatedQuery() {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder( "not " );
		builder.append( getQuery() );
		return builder;
	}
}
