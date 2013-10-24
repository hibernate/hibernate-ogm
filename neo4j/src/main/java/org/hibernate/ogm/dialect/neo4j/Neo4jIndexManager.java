/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.neo4j;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Manages {@link Node} and {@link Relationship} indexes.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jIndexManager {

	private static final String TABLE_PROPERTY = "_table";
	private static final String RELATIONSHIP_TYPE = "_relationship_type";

	private final Neo4jDatastoreProvider provider;

	public Neo4jIndexManager(Neo4jDatastoreProvider provider) {
		this.provider = provider;
	}

	/**
	 * Index a {@link Node}.
	 *
	 * @see Neo4jIndexManager#findNode(EntityKey)
	 * @param node
	 *            the Node to index
	 * @param entityKey
	 *            the {@link EntityKey} representing the node
	 */
	public void index(Node node, EntityKey entityKey) {
		Index<Node> nodeIndex = provider.getNodesIndex();
		nodeIndex.add( node, TABLE_PROPERTY, entityKey.getTable() );
		for ( int i = 0; i < entityKey.getColumnNames().length; i++ ) {
			nodeIndex.add( node, entityKey.getColumnNames()[i], entityKey.getColumnValues()[i] );
		}
	}

	private Map<String, Object> properties(EntityKey entitykey) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( TABLE_PROPERTY, entitykey.getTable() );
		for ( int j = 0; j < entitykey.getColumnNames().length; j++ ) {
			properties.put( entitykey.getColumnNames()[j], entitykey.getColumnValues()[j] );
		}
		return properties;
	}

	/**
	 * Index a {@link Relationship}.
	 *
	 * @see Neo4jIndexManager#findRelationship(RelationshipType, RowKey)
	 * @param relationship
	 *            the Relationship to index
	 */
	public void index(Relationship relationship) {
		Index<Relationship> relationshipIndex = provider.getRelationshipsIndex();
		relationshipIndex.add( relationship, RELATIONSHIP_TYPE, relationship.getType().name() );
		for ( String key : relationship.getPropertyKeys() ) {
			relationshipIndex.add( relationship, key, relationship.getProperty( key ) );
		}
	}

	/**
	 * Remove a {@link Relationship} from the index.
	 *
	 * @param rel
	 *            the Relationship is going to be removed from the index
	 *
	 */
	public void remove(Relationship rel) {
		Index<Relationship> relationshipIndex = provider.getRelationshipsIndex();
		relationshipIndex.remove( rel );
	}

	/**
	 * Looks for a {@link Relationship} in the index.
	 *
	 * @param type
	 *            the {@link RelationshipType} of the wanted relationship
	 * @param rowKey
	 *            the {@link RowKey} that representing the relationship.
	 * @return the relationship found or null
	 */
	public Relationship findRelationship(RelationshipType type, RowKey rowKey) {
		String query = createQuery( properties( type, rowKey ) );
		Index<Relationship> relationshipIndex = provider.getRelationshipsIndex();
		return relationshipIndex.query( query ).getSingle();
	}

	private Map<String, Object> properties(RelationshipType type, RowKey rowKey) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( RELATIONSHIP_TYPE, type.name() );
		for ( int i = 0; i < rowKey.getColumnNames().length; i++ ) {
			properties.put( rowKey.getColumnNames()[i], rowKey.getColumnValues()[i] );
		}
		return properties;
	}

	/**
	 * Looks for a {@link Node} in the index.
	 *
	 * @param entityKey
	 *            the {@link EntityKey} that identify the node.
	 * @return the node found or null
	 */
	public Node findNode(EntityKey entityKey) {
		String query = createQuery( properties( entityKey ) );
		Index<Node> nodeIndex = provider.getNodesIndex();
		return nodeIndex.query( query ).getSingle();
	}

	private String createQuery(Map<String, Object> properties) {
		StringBuilder queryBuilder = new StringBuilder();
		for ( Map.Entry<String, Object> entry : properties.entrySet() ) {
			queryBuilder.append( " AND " );
			appendTerm( queryBuilder, entry.getKey(), entry.getValue() );
		}
		return queryBuilder.substring( " AND ".length() );
	}

	private void appendTerm(StringBuilder queryBuilder, String key, Object value) {
		queryBuilder.append( key );
		queryBuilder.append( ": \"" );
		queryBuilder.append( value );
		queryBuilder.append( "\"" );
	}

	public void remove(Node entityNode) {
		Index<Node> nodeIndex = provider.getNodesIndex();
		nodeIndex.remove( entityNode );
	}

	/**
	 * Return all the indexed nodes  corresponding to an entity type.
	 *
	 * @param tableName
	 *            the name of the table representing the entity
	 * @return the nodes representing the entities
	 */
	public IndexHits<Node> findNodes(String tableName) {
		Index<Node> nodeIndex = provider.getNodesIndex();
		return nodeIndex.get( TABLE_PROPERTY, tableName );
	}

}
