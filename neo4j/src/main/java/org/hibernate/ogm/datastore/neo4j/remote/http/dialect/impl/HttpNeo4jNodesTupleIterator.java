/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;

/**
 * Iterates over the result of a native query when each result is a neo4j node. This is the case when the result of
 * native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class HttpNeo4jNodesTupleIterator implements ClosableIterator<Tuple> {

	private final EntityKeyMetadata entityKeyMetadata;
	private final Long txId;
	private final TupleTypeContext tupleTypeContext;
	private final ClosableIterator<NodeWithEmbeddedNodes> entities;
	private final HttpNeo4jEntityQueries entityQueries;
	private final HttpNeo4jClient client;

	public HttpNeo4jNodesTupleIterator(
			HttpNeo4jClient client,
			Long txId,
			HttpNeo4jEntityQueries entityQueries,
			StatementsResponse response,
			EntityKeyMetadata entityKeyMetadata,
			TupleTypeContext tupleTypeContext,
			ClosableIterator<NodeWithEmbeddedNodes> entities) {
		this.client = client;
		this.txId = txId;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleTypeContext = tupleTypeContext;
		this.entities = entities;
	}

	private Tuple createTuple(NodeWithEmbeddedNodes node) {
		Map<String, Node> associatedNodes = HttpNeo4jAssociatedNodesHelper.findAssociatedNodes( client, txId, node, entityKeyMetadata, tupleTypeContext,
				entityQueries );
		return new Tuple( new HttpNeo4jTupleSnapshot( node, entityKeyMetadata, associatedNodes, tupleTypeContext ), SnapshotType.UPDATE );
	}

	@Override
	public boolean hasNext() {
		return entities.hasNext();
	}

	@Override
	public Tuple next() {
		return createTuple( entities.next() );
	}

	@Override
	public void remove() {
		entities.remove();
	}

	@Override
	public void close() {
		entities.close();
	}

}
