/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import static org.hibernate.ogm.util.impl.EmbeddedHelper.isPartOfEmbedded;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Davide D'Alto
 */
public class RemoteNeo4jTupleAssociationSnapshot implements TupleSnapshot {

	private final Map<String, Object> properties;

	private SnapshotType snapshotType = SnapshotType.UNKNOWN;

	public RemoteNeo4jTupleAssociationSnapshot(RemoteNeo4jClient neo4jClient, RemoteNeo4jAssociationQueries queries, RemoteNeo4jAssociationPropertiesRow row, AssociationKey associationKey, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		this.properties = collectProperties( neo4jClient, queries, row, associationKey, associatedEntityKeyMetadata );
	}

	private static Map<String, Object> collectProperties(RemoteNeo4jClient client, RemoteNeo4jAssociationQueries queries, RemoteNeo4jAssociationPropertiesRow row, AssociationKey associationKey, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {

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
				// Embedded column
				String collectionRole = associationKey.getMetadata().getCollectionRole();
				if ( targetColumnName.equals( collectionRole ) ) {
					// Ex: @ElementCollection List<String> examples
					targetColumnName = targetColumnName.substring( targetColumnName.lastIndexOf( "." ) + 1 );
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

	@Override
	public SnapshotType getSnapshotType() {
		return snapshotType;
	}

	@Override
	public void setSnapshotType(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
	}

}
