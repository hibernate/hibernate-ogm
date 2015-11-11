/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

/**
 * Iterates over the result of a native query when each result is a neo4j node.
 * This is the case when the result of native query is mapped by an entity type.
 *
 * @author Davide D'Alto
 */
public class RemoteNodesTupleIterator extends RemoteMapsTupleIterator {

	private final EntityKeyMetadata entityKeyMetadata;
	private final Neo4jClient dataBase;
	private final RemoteNeo4jEntityQueries entityQueries;
	private final OgmEntityPersister ogmEntityPersister;

	public RemoteNodesTupleIterator(Neo4jClient dataBase, RemoteNeo4jEntityQueries entityQueries, StatementsResponse result, EntityKeyMetadata entityKeyMetadata, OgmEntityPersister ogmEntityPersister) {
		super( result );
		this.dataBase = dataBase;
		this.entityQueries = entityQueries;
		this.entityKeyMetadata = entityKeyMetadata;
		this.ogmEntityPersister = ogmEntityPersister;
	}

	protected Tuple convert(Row next) {
		Node node = next.getGraph().getNodes().get( 0 );
		return createTuple( node );
	}

	private Tuple createTuple(Node node) {
		return new Tuple( new RemoteNeo4jTupleSnapshot( dataBase, entityQueries, node, entityKeyMetadata, ogmEntityPersister ) );
	}
}
