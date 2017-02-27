/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * @author Davide D'Alto
 */
public final class HttpNeo4jAssociatedNodesHelper {

	private HttpNeo4jAssociatedNodesHelper() {
	}

	public static Map<String, Node> findAssociatedNodes(HttpNeo4jClient client, Long txId, NodeWithEmbeddedNodes node, EntityKeyMetadata entityKeyMetadata,
			TupleTypeContext tupleTypeContext, HttpNeo4jEntityQueries queries) {
		Map<String, Node> associatedNodes = new HashMap<>( tupleTypeContext.getAllAssociatedEntityKeyMetadata().size() );
		if ( tupleTypeContext.getAllAssociatedEntityKeyMetadata().size() > 0 ) {
			Object[] keyValues = keyValues( node.getOwner(), entityKeyMetadata );
			for ( Entry<String, AssociatedEntityKeyMetadata> entry : tupleTypeContext.getAllAssociatedEntityKeyMetadata().entrySet() ) {
				String associationRole = tupleTypeContext.getAllRoles().get( entry.getKey() );
				Node associatedEntity = queries.findAssociatedEntity( client, txId, keyValues, associationRole );
				associatedNodes.put( associationRole, associatedEntity );
			}
		}
		return associatedNodes;
	}

	private static Object[] keyValues(Node node, EntityKeyMetadata entityKeyMetadata) {
		Object[] values = new Object[entityKeyMetadata.getColumnNames().length];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = node.getProperties().get( entityKeyMetadata.getColumnNames()[i] );
		}
		return values;
	}
}
