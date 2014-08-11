/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * Represents the Tuple snapshot as loaded by the Neo4j datastore.
 * <p>
 * The columns of the tuple are mapped as properties of the node.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public final class Neo4jTupleSnapshot implements TupleSnapshot {

	private final Node node;
	private final TupleContext tupleContext;

	public Neo4jTupleSnapshot(Node node) {
		this( node, null);
	}

	public Neo4jTupleSnapshot(Node node, TupleContext tupleContext) {
		this.node = node;
		this.tupleContext = tupleContext;
	}

	@Override
	public Object get(String column) {
		if ( tupleContext != null && tupleContext.getAssociatedEntitiesMetadata().isForeignKeyColumn( column ) ) {
			return readPropertyOnOtherNode( column );
		}
		else {
			return readProperty( node, column );
		}
	}

	private Object readPropertyOnOtherNode(String column) {
		String tableName = tupleContext.getAssociatedEntitiesMetadata().getTargetEntityTable( column );
		String targetColumnName = tupleContext.getAssociatedEntitiesMetadata().getTargetColumnName( column );
		for ( Relationship relationship : node.getRelationships() ) {
			Node otherNode = relationship.getOtherNode( node );
			if ( otherNode.hasLabel( CypherCRUD.nodeLabel( tableName ) ) ) {
				Object value = readProperty( otherNode, targetColumnName );
				return value;
			}

		}
		return null;
	}

	private Object readProperty(Node otherNode, String targetColumnName) {
		Object value = null;
		if ( otherNode.hasProperty( targetColumnName ) ) {
			value = otherNode.getProperty( targetColumnName );
		}
		return value;
	}

	@Override
	public boolean isEmpty() {
		return !node.getPropertyKeys().iterator().hasNext();
	}

	@Override
	public Set<String> getColumnNames() {
		Set<String> names = new HashSet<String>();
		for ( String string : node.getPropertyKeys() ) {
			names.add( string );
		}
		return names;
	}

	public Node getNode() {
		return node;
	}

}
