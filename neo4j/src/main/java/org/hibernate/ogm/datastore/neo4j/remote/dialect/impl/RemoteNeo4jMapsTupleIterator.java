/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;

/**
 * Iterates over the results of a native query when each result is not mapped by an entity
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jMapsTupleIterator implements ClosableIterator<Tuple> {

	private final Iterator<Row> iterator;
	private final List<String> columns;

	public RemoteNeo4jMapsTupleIterator(StatementsResponse response) {
		StatementResult result = response.getResults().get( 0 );
		List<Row> rows = result.getData();
		this.columns = result.getColumns();
		this.iterator = rows.iterator();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Tuple next() {
		Row row = iterator.next();
		return convert( row );
	}

	protected Tuple convert(Row next) {
		// Requires a LinkedHashMap as the order of the entries is important
		Map<String, Object> properties = new LinkedHashMap<>();
		for ( int i = 0; i < columns.size(); i++ ) {
			properties.put( columns.get( i ), next.getRow().get( i ) );
		}
		return new Tuple( new MapTupleSnapshot( properties ), SnapshotType.UPDATE );
	}

	@Override
	public void remove() {
		iterator.remove();
	}

	@Override
	public void close() {
	}
}
