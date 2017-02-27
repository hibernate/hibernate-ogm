/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.neo4j.BaseNeo4jDialect;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.neo4j.driver.v1.types.Node;

/**
 * Represents the Tuple snapshot as loaded by the Neo4j datastore.
 * <p>
 * The columns of the tuple are mapped as properties of the node.
 *
 * @author Davide D'Alto
 */
public final class BoltNeo4jTupleSnapshot implements TupleSnapshot {

	private final Node node;
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata;
	private final EntityKeyMetadata entityKeyMetadata;

	private final Map<String, Node> toOneEntities;
	private final Map<String, Collection<Node>> embeddedNodes;
	private final Map<String, String> rolesByColumn;

	public BoltNeo4jTupleSnapshot(
			NodeWithEmbeddedNodes node,
			EntityKeyMetadata entityKeyMetadata) {
		this( node, entityKeyMetadata, Collections.<String, Node>emptyMap(), null );
	}

	public BoltNeo4jTupleSnapshot(
			NodeWithEmbeddedNodes node,
			EntityKeyMetadata metadata,
			Map<String, Node> toOneEntities,
			TupleTypeContext tupleTypeContext
			) {
		this.node = node.getOwner();
		this.embeddedNodes = node.getEmbeddedNodes();
		this.entityKeyMetadata = metadata;
		this.toOneEntities = toOneEntities;
		this.associatedEntityKeyMetadata = tupleTypeContext.getAllAssociatedEntityKeyMetadata();
		this.rolesByColumn = tupleTypeContext.getAllRoles();
	}

	@Override
	public Object get(String column) {
		if ( associatedEntityKeyMetadata.containsKey( column ) ) {
			return readPropertyOnOtherNode( column );
		}
		else if ( BaseNeo4jDialect.isPartOfRegularEmbedded( entityKeyMetadata.getColumnNames(), column ) ) {
			return readEmbeddedProperty( column );
		}
		else {
			return readProperty( node, column );
		}
	}

	private Object readPropertyOnOtherNode(String column) {
		String associationrole = rolesByColumn.get( column );
		Node associatedEntity = toOneEntities.get( associationrole );
		if ( associatedEntity == null ) {
			return null;
		}

		return readProperty( associatedEntity, associatedEntityKeyMetadata.get( column ).getCorrespondingEntityKeyColumn( column ) );
	}

	private Object readEmbeddedProperty(String column) {
		String embeddedPath = column.substring( 0, column.lastIndexOf( "." ) );
		String property = column.substring( embeddedPath.length() + 1 );
		if ( embeddedNodes.containsKey( embeddedPath ) ) {
			Node embeddedNode = embeddedNodes.get( embeddedPath ).iterator().next();
			return readProperty( embeddedNode, property );
		}
		return null;
	}

	private Object readProperty(Node node, String targetColumnName) {
		if ( node.containsKey( targetColumnName ) ) {
			return node.get( targetColumnName ).asObject();
		}
		return null;
	}

	@Override
	public boolean isEmpty() {
		return node.asMap().isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		Set<String> names = new HashSet<String>();
		for ( String string : node.keys() ) {
			names.add( string );
		}
		return names;
	}

	public Node getNode() {
		return node;
	}
}
