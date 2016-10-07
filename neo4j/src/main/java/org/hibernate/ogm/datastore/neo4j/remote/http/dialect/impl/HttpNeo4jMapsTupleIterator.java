/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jMapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementResult;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * Iterates over the results of a native query when each result is not mapped by an entity
 *
 * @author Davide D'Alto
 */
public class HttpNeo4jMapsTupleIterator extends RemoteNeo4jMapsTupleIterator<Row> {

	public HttpNeo4jMapsTupleIterator(StatementResult result) {
		super( result.getData().iterator(), result.getColumns() );
	}

	@Override
	protected TupleSnapshot tupleSnapshot(Row next, List<String> keys) {
		// Requires a LinkedHashMap as the order of the entries is important
		Map<String, Object> properties = new LinkedHashMap<>();
		for ( int i = 0; i < keys.size(); i++ ) {
			properties.put( keys.get( i ), next.getRow().get( i ) );
		}
		return new MapTupleSnapshot( properties );
	}
}
