/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl;

import static org.hibernate.ogm.util.impl.EmbeddedHelper.isPartOfEmbedded;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * A {@link TupleSnapshot} of the row in an association for remote Neo4j dialects.
 *
 * @author Davide D'Alto
 */

public class RemoteNeo4jTupleAssociationSnapshot implements TupleSnapshot {

	private final Map<String, Object> properties;

	public RemoteNeo4jTupleAssociationSnapshot(RemoteNeo4jAssociationPropertiesRow row, AssociationKey associationKey, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		this.properties = collectProperties( row, associationKey, associatedEntityKeyMetadata );
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

	private static Map<String, Object> collectProperties(RemoteNeo4jAssociationPropertiesRow row, AssociationKey associationKey,
			AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {

		Map<String, Object> properties = new HashMap<String, Object>();
		String[] rowKeyColumnNames = associationKey.getMetadata().getRowKeyColumnNames();

		Map<String, Object> relationship = row.getRelationship();
		Map<String, Object> ownerNode = row.getOwnerNode();
		Map<String, Object> targetNode = row.getTargetNode();

		// Index columns
		for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
			if ( relationship.containsKey( rowKeyColumnNames[i] ) ) {
				properties.put( rowKeyColumnNames[i], relationship.get( rowKeyColumnNames[i] ) );
			}
		}

		// Properties stored in the target side of the association
		for ( String associationColumn : associatedEntityKeyMetadata.getAssociationKeyColumns() ) {
			String targetColumnName = associatedEntityKeyMetadata.getCorrespondingEntityKeyColumn( associationColumn );
			if ( isPartOfEmbedded( targetColumnName ) ) {
				fetchEmbeddedProperties( associationKey, properties, targetNode, associationColumn, targetColumnName );
			}
			else {
				if ( targetNode.containsKey( targetColumnName ) ) {
					properties.put( associationColumn, targetNode.get( targetColumnName ) );
				}
			}
		}

		// Property stored in the owner side of the association
		for ( int i = 0; i < associationKey.getColumnNames().length; i++ ) {
			String key = associationKey.getEntityKey().getColumnNames()[i];
			if ( ownerNode.containsKey( key ) ) {
				properties.put( associationKey.getColumnNames()[i], ownerNode.get( key ) );
			}
		}
		return properties;
	}

	private static void fetchEmbeddedProperties(AssociationKey associationKey, Map<String, Object> properties, Map<String, Object> targetNode,
			String associationColumn,
			String targetColumnName) {
		// Embedded column
		String collectionRole = associationKey.getMetadata().getCollectionRole();
		if ( targetColumnName.equals( collectionRole ) ) {
			// Ex: @ElementCollection List<String> examples
			targetColumnName = targetColumnName.substring( targetColumnName.lastIndexOf( "." ) + 1 );
			if ( targetNode.containsKey( targetColumnName ) ) {
				properties.put( associationColumn, targetNode.get( targetColumnName ) );
			}
		}
		if ( targetColumnName.startsWith( collectionRole ) ) {
			// Ex: @ElementCollection Map<String, Embedded> examples
			targetColumnName = targetColumnName.substring( collectionRole.length() + 1 );
			if ( targetNode.containsKey( targetColumnName ) ) {
				properties.put( associationColumn, targetNode.get( targetColumnName ) );
			}
		}
		else if ( targetNode.containsKey( targetColumnName ) ) {
			// Embedded id
			properties.put( associationColumn, targetNode.get( targetColumnName ) );
		}
		else {
			// Ex: @ElementCollection List<Embedded> examples

			if ( targetNode.containsKey( targetColumnName ) ) {
				properties.put( associationColumn, targetNode.get( targetColumnName ) );
			}
		}
	}
}
