/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.neo4j.graphdb.Direction;
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
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata;
	private final Map<String, String> rolesByColumn;

	public Neo4jTupleSnapshot(Node node) {
		this( node, Collections.<String, AssociatedEntityKeyMetadata>emptyMap(), Collections.<String, String>emptyMap() );
	}

	public Neo4jTupleSnapshot(Node node, Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata, Map<String, String> rolesByColumn) {
		this.node = node;
		this.associatedEntityKeyMetadata = associatedEntityKeyMetadata;
		this.rolesByColumn = rolesByColumn;
	}

	@Override
	public Object get(String column) {
		if ( associatedEntityKeyMetadata.containsKey( column ) ) {
			return readPropertyOnOtherNode( column );
		}
		else {
			return readProperty( node, column );
		}
	}

	private Object readPropertyOnOtherNode(String column) {
		Iterator<Relationship> relationships = node.getRelationships( Direction.OUTGOING, withName( rolesByColumn.get( column ) ) ).iterator();

		if ( relationships.hasNext() ) {
			Node otherNode = relationships.next().getEndNode();
			return readProperty( otherNode, associatedEntityKeyMetadata.get( column ).getCorrespondingEntityKeyColumn( column ) );
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
