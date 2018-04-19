package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.RootPredicate;

public class InfinispanRemoteRootPredicate extends RootPredicate<StringBuilder> {

	private final String table;
	private final String protobufPackage;

	public InfinispanRemoteRootPredicate(String table, String protobufPackage) {
		this.table = table;
		this.protobufPackage = protobufPackage;
	}

	@Override
	public StringBuilder getQuery() {
		StringBuilder query = new StringBuilder( "from " );
		query.append( protobufPackage );
		query.append( "." );
		query.append( table );

		if ( child == null ) {
			return query;
		}

		query.append( " where " );
		query.append( child.getQuery() );

		return query;
	}
}
