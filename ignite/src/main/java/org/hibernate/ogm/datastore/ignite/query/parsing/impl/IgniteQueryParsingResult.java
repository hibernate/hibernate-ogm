package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryDescriptor;
import org.hibernate.ogm.query.spi.QueryParsingResult;

/**
 * Result of parsing HQL-query for Ignite
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryParsingResult implements QueryParsingResult {

	private final IgniteQueryDescriptor query;
	private final List<String> columnNames;
	
	public IgniteQueryParsingResult(IgniteQueryDescriptor query, List<String> columnNames) {
		this.query = query;
		if (columnNames == null || columnNames.isEmpty())
			this.columnNames = Collections.emptyList();
		else
			this.columnNames = columnNames;
	}
	
	@Override
	public IgniteQueryDescriptor getQueryObject() {
		return query;
	}

	@Override
	public List<String> getColumnNames() {
		return columnNames;
	}

}
