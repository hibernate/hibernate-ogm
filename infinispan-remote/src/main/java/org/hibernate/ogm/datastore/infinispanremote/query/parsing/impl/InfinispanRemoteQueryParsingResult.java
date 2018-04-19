package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.util.List;

import org.hibernate.ogm.query.spi.QueryParsingResult;

import org.infinispan.query.dsl.Query;

public class InfinispanRemoteQueryParsingResult implements QueryParsingResult {

	private final String query;
	private final List<String> projections;

	public InfinispanRemoteQueryParsingResult(String query, List<String> projections) {
		this.query = query;
		this.projections = projections;
	}

	@Override
	public Object getQueryObject() {
		return query;
	}

	@Override
	public List<String> getColumnNames() {
		return projections;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "InfinispanRemoteQueryParsingResult{" );
		sb.append( ", query='" ).append( query ).append( '\'' );
		sb.append( ", projections=" ).append( projections );
		sb.append( '}' );
		return sb.toString();
	}
}
