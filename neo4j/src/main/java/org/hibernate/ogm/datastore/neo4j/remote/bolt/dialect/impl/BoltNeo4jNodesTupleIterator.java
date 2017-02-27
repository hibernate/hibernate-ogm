/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;

/**
 * Iterates over the result of a native query when each result is a neo4j node. This is the case when the result of
 * native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class BoltNeo4jNodesTupleIterator implements ClosableIterator<Tuple> {

	private final EntityKeyMetadata entityKeyMetadata;
	private final BoltNeo4jEntityQueries entityQueries;
	private final TupleTypeContext tupleTypeContext;
	private final ClosableIterator<NodeWithEmbeddedNodes> entities;
	private final Transaction tx;
	private final boolean closeTransaction;

	public BoltNeo4jNodesTupleIterator(
			Transaction tx,
			BoltNeo4jEntityQueries entityQueries,
			EntityKeyMetadata entityKeyMetadata,
			TupleTypeContext tupleTypeContext,
			ClosableIterator<NodeWithEmbeddedNodes> entities) {
		this( tx, entityQueries, entityKeyMetadata, tupleTypeContext, entities, false );
	}

	public BoltNeo4jNodesTupleIterator(
			Transaction tx,
			BoltNeo4jEntityQueries entityQueries,
			EntityKeyMetadata entityKeyMetadata,
			TupleTypeContext tupleTypeContext,
			ClosableIterator<NodeWithEmbeddedNodes> entities,
			boolean closeTransaction) {
		this.tx = tx;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleTypeContext = tupleTypeContext;
		this.entities = entities;
		this.closeTransaction = closeTransaction;
	}

	private Tuple createTuple(NodeWithEmbeddedNodes node) {
		Map<String, Node> toOneEntities = BoltNeo4jAssociatedNodesHelper.findAssociatedNodes( tx, node, entityKeyMetadata, tupleTypeContext, entityQueries );
		return new Tuple( new BoltNeo4jTupleSnapshot( node, entityKeyMetadata, toOneEntities, tupleTypeContext ), SnapshotType.UPDATE );
	}

	@Override
	public boolean hasNext() {
		return entities.hasNext();
	}

	@Override
	public Tuple next() {
		NodeWithEmbeddedNodes node = entities.next();
		return createTuple( node );
	}

	@Override
	public void remove() {
		entities.remove();
	}

	@Override
	public void close() {
		try {
			entities.close();
			if ( closeTransaction ) {
				tx.success();
			}
		}
		finally {
			if ( closeTransaction ) {
				tx.close();
			}
		}
	}
}
