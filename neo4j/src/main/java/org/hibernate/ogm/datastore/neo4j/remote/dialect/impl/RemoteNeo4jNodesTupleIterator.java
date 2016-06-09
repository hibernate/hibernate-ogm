/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Iterates over the result of a native query when each result is a neo4j node. This is the case when the result of
 * native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jNodesTupleIterator extends RemoteNeo4jMapsTupleIterator {

	private final EntityKeyMetadata entityKeyMetadata;
	private final RemoteNeo4jClient dataBase;
	private final RemoteNeo4jEntityQueries entityQueries;
	private final Long txId;
	private final TupleContext tupleContext;

	public RemoteNeo4jNodesTupleIterator(RemoteNeo4jClient dataBase,
			Long txId,
			RemoteNeo4jEntityQueries entityQueries,
			StatementsResponse result,
			EntityKeyMetadata entityKeyMetadata,
			TupleContext tupleContext) {
		super( result );
		this.dataBase = dataBase;
		this.txId = txId;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.tupleContext = tupleContext;
	}

	protected Tuple convert(Row next) {
		Node node = next.getGraph().getNodes().get( 0 );
		return createTuple( node );
	}

	private Tuple createTuple(Node node) {
		return new Tuple( new RemoteNeo4jTupleSnapshot( dataBase, txId, entityQueries, node, tupleContext.getAllAssociatedEntityKeyMetadata(),
				tupleContext.getAllRoles(), entityKeyMetadata ) );
	}
}
