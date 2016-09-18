/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
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
	private final TupleContext tupleContext;
	private final ClosableIterator<NodeWithEmbeddedNodes> entities;
	private final Transaction tx;

	public BoltNeo4jNodesTupleIterator(
			Transaction tx,
			BoltNeo4jEntityQueries entityQueries,
			EntityKeyMetadata entityKeyMetadata,
			TupleContext tupleContext,
			ClosableIterator<NodeWithEmbeddedNodes> entities) {
		this.tx = tx;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleContext = tupleContext;
		this.entities = entities;
	}

	private Tuple createTuple(NodeWithEmbeddedNodes node) {
		Map<String, Node> toOneEntities = BoltNeo4jAssociatedNodesHelper.findAssociatedNodes( tx, node, entityKeyMetadata, tupleContext, entityQueries );
		return new Tuple( new BoltNeo4jTupleSnapshot( node, entityKeyMetadata, toOneEntities, tupleContext ) );
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
		entities.close();
	}
}
