/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.RootPredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;

/**
 * Infinispan server-based implementation of {@link RootPredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteRootPredicate extends RootPredicate<InfinispanRemoteQueryBuilder> {

	private final String table;
	private final String protobufPackage;

	public InfinispanRemoteRootPredicate(String table, String protobufPackage) {
		this.table = table;
		this.protobufPackage = protobufPackage;
	}

	@Override
	public InfinispanRemoteQueryBuilder getQuery() {
		InfinispanRemoteQueryBuilder query = new InfinispanRemoteQueryBuilder( "from " );
		query.append( protobufPackage );
		query.append( "." );
		query.append( table );

		if ( child == null ) {
			return query;
		}

		query.addWhere( child.getQuery() );

		return query;
	}
}
