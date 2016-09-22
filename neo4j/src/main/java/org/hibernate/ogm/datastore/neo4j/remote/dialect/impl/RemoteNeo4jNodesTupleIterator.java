/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Iterates over the result of a native query when each result is a neo4j node. This is the case when the result of
 * native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jNodesTupleIterator implements ClosableIterator<Tuple> {

	private final EntityKeyMetadata entityKeyMetadata;
	private final RemoteNeo4jClient dataBase;
	private final RemoteNeo4jEntityQueries entityQueries;
	private final Long txId;
	private final TupleContext tupleContext;
	private final ClosableIterator<NodeWithEmbeddedNodes> entities;

	public RemoteNeo4jNodesTupleIterator(RemoteNeo4jClient dataBase,
			Long txId,
			RemoteNeo4jEntityQueries entityQueries,
			StatementsResponse response,
			EntityKeyMetadata entityKeyMetadata,
			TupleContext tupleContext,
			ClosableIterator<NodeWithEmbeddedNodes> entities) {
		this.dataBase = dataBase;
		this.txId = txId;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleContext = tupleContext;
		this.entities = entities;
	}

	private Tuple createTuple(NodeWithEmbeddedNodes node) {
		return new Tuple(
				new RemoteNeo4jTupleSnapshot( dataBase, txId, entityQueries, node, tupleContext.getAllAssociatedEntityKeyMetadata(),
						tupleContext.getAllRoles(), entityKeyMetadata ) );
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
