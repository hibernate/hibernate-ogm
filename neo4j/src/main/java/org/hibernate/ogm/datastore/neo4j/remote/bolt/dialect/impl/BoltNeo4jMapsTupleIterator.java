/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jMapsTupleIterator;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;

/**
 * Iterates over the results of a native query when each result is not mapped by an entity
 *
 * @author Davide D'Alto
 */
public class BoltNeo4jMapsTupleIterator extends RemoteNeo4jMapsTupleIterator<Record> {

	public BoltNeo4jMapsTupleIterator(StatementResult statementResult) {
		super( statementResult, statementResult.keys() );
	}

	@Override
	protected TupleSnapshot tupleSnapshot(Record record, List<String> keys) {
		// Requires a LinkedHashMap as the order of the entries is important
		Map<String, Object> properties = new LinkedHashMap<>();
		for ( String column : keys ) {
			Value value = record.get( column );
			if ( value != null && !value.isNull() ) {
				properties.put( column, value.asObject() );
			}
			else {
				properties.put( column, null );
			}
		}
		return new MapTupleSnapshot( properties );
	}
}
