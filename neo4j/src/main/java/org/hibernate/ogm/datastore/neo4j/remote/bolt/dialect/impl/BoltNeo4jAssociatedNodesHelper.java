/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Node;

/**
 * Function required to get the associated nodes when building the tuple snapshot.
 *
 * @author Davide D'Alto
 */
public final class BoltNeo4jAssociatedNodesHelper {

	private BoltNeo4jAssociatedNodesHelper() {
	}

	public static Map<String, Node> findAssociatedNodes(Transaction tx, NodeWithEmbeddedNodes node, EntityKeyMetadata entityKeyMetadata,
			TupleTypeContext tupleTypeContext, BoltNeo4jEntityQueries queries) {
		Map<String, Node> associatedNodes = new HashMap<>( tupleTypeContext.getAllAssociatedEntityKeyMetadata().size() );
		if ( tupleTypeContext.getAllAssociatedEntityKeyMetadata().size() > 0 ) {
			Object[] keyValues = keyValues( node.getOwner(), entityKeyMetadata );
			for ( Entry<String, AssociatedEntityKeyMetadata> entry : tupleTypeContext.getAllAssociatedEntityKeyMetadata().entrySet() ) {
				String associationRole = tupleTypeContext.getAllRoles().get( entry.getKey() );
				Node associatedEntity = queries.findAssociatedEntity( tx, keyValues, associationRole );
				associatedNodes.put( associationRole, associatedEntity );
			}
		}
		return associatedNodes;
	}

	private static Object[] keyValues(Node node, EntityKeyMetadata entityKeyMetadata) {
		Object[] values = new Object[entityKeyMetadata.getColumnNames().length];
		for ( int i = 0; i < values.length; i++ ) {
			values[i] = node.get( entityKeyMetadata.getColumnNames()[i] );
		}
		return values;
	}
}
