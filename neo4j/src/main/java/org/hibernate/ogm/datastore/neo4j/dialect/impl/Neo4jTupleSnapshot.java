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
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
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

	private static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	private final Node node;
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata;
	private final Map<String, String> rolesByColumn;
	private final EntityKeyMetadata entityKeyMetadata;

	public Neo4jTupleSnapshot(Node node, EntityKeyMetadata entityKeyMetadata) {
		this( node, Collections.<String, AssociatedEntityKeyMetadata>emptyMap(), Collections.<String, String>emptyMap(), entityKeyMetadata );
	}

	public Neo4jTupleSnapshot(Node node, Map<String, AssociatedEntityKeyMetadata> associatedEntityKeyMetadata, Map<String, String> rolesByColumn, EntityKeyMetadata entityKeyMetadata) {
		this.node = node;
		this.associatedEntityKeyMetadata = associatedEntityKeyMetadata;
		this.rolesByColumn = rolesByColumn;
		this.entityKeyMetadata = entityKeyMetadata;
	}

	@Override
	public Object get(String column) {
		if ( associatedEntityKeyMetadata.containsKey( column ) ) {
			return readPropertyOnOtherNode( column );
		}
		else if ( Neo4jDialect.isPartOfRegularEmbedded( entityKeyMetadata.getColumnNames(), column ) ) {
			return readEmbeddedProperty( column );
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

	// TODO: We should create a query to read this value
	private Object readEmbeddedProperty(String column) {
		String[] split = EMBEDDED_FIELDNAME_SEPARATOR.split( column );
		Node embeddedNode = node;
		for ( int i = 0; i < split.length - 1; i++ ) {
			String relType = split[i];
			Iterator<Relationship> rels = embeddedNode.getRelationships( Direction.OUTGOING, withName( relType ) ).iterator();
			if ( rels.hasNext() ) {
				embeddedNode = rels.next().getEndNode();
			}
			else {
				return null;
			}
		}
		return readProperty( embeddedNode, split[split.length - 1] );
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
