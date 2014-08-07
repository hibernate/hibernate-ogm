/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD.nodeLabel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * @author Davide D'Alto
 */
public class Neo4jTupleAssociationSnapshot implements TupleSnapshot {

	private final Map<String, Object> properties;

	public Neo4jTupleAssociationSnapshot(Relationship relationship, AssociationKey associationKey, AssociationContext associationContext) {
		properties = collectProperties( relationship, associationKey, associationContext );
	}

	private Map<String, Object> collectProperties(Relationship relationship, AssociationKey associationKey, AssociationContext associationContext) {
		Map<String, Object> properties = new HashMap<String, Object>();
		String[] rowKeyColumnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Node ownerNode = ownerNode( associationKey, relationship );
		Node targetNode = relationship.getOtherNode( ownerNode );

		// Index columns
		for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
			if ( relationship.hasProperty( rowKeyColumnNames[i] ) ) {
				properties.put( rowKeyColumnNames[i], relationship.getProperty( rowKeyColumnNames[i] ) );
			}
		}

		// Properties stored in the target side of the association
		String[] targetColumnNames = associationKey.getMetadata().getRowKeyEntityKeyMetadata().getColumnNames();
		String[] associationTargetColumnNames = associationKey.getMetadata().getRowKeyTargetAssociationKeyColumnNames();
		for ( int i = 0; i < associationTargetColumnNames.length; i++ ) {
			if ( targetNode.hasProperty( targetColumnNames[i] ) ) {
				properties.put( associationTargetColumnNames[i], targetNode.getProperty( targetColumnNames[i] ) );
			}
		}

		// Property stored in the owner side of the association
		for ( int i = 0; i < associationKey.getColumnNames().length; i++ ) {
			if ( ownerNode.hasProperty( associationKey.getEntityKey().getColumnNames()[i] ) ) {
				properties.put( associationKey.getColumnNames()[i], ownerNode.getProperty( associationKey.getEntityKey().getColumnNames()[i] ) );
			}
		}
		return properties;
	}

	private static Node ownerNode(AssociationKey associationKey, Relationship relationship) {
		if ( relationship.getStartNode().hasLabel( nodeLabel( associationKey.getEntityKey() ) ) ) {
			return relationship.getStartNode();
		}
		else {
			return relationship.getEndNode();
		}
	}

	@Override
	public Object get(String column) {
		return properties.get( column );
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return properties.keySet();
	}

}
