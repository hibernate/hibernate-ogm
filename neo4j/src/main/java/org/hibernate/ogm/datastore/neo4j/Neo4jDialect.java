/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD.relationshipType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Abstracts Hibernate OGM from Neo4j.
 * <p>
 * A {@link Tuple} is saved as a {@link Node} where the columns are converted into properties of the node.<br>
 * An {@link Association} is converted into a {@link Relationship} identified by the {@link AssociationKey} and the
 * {@link RowKey}. The type of the relationship is the value returned by {@link AssociationKey#getCollectionRole()}. An
 * additional property containing the value in {@link AssociationKey#getTable()} is added to the relationship.
 * <p>
 * If the value of a property is set to null the propety will be removed (Neo4j does not allow to store null values).
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jDialect implements GridDialect {

	private final CypherCRUD neo4jCRUD;

	private final Neo4jSequenceGenerator neo4jSequenceGenerator;

	public Neo4jDialect(Neo4jDatastoreProvider provider) {
		this.neo4jCRUD = new CypherCRUD( provider.getDataBase() );
		this.neo4jSequenceGenerator = new Neo4jSequenceGenerator( provider.getDataBase() );
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Neo4j GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext context) {
		Node entityNode = neo4jCRUD.findNode( key );
		if ( entityNode == null ) {
			return null;
		}
		return createTuple( entityNode );
	}

	private static Tuple createTuple(Node entityNode) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple();
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Node node = neo4jCRUD.createNodeUnlessExists( key );
		applyTupleOperations( node, tuple.getOperations() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		neo4jCRUD.remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		Node entityNode = neo4jCRUD.findNode( associationKey.getEntityKey() );
		if ( entityNode == null ) {
			return null;
		}
		return new Association( new Neo4jAssociationSnapshot( entityNode, associationKey ) );
	}

	@Override
	public Association createAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		return new Association();
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		for ( AssociationOperation action : association.getOperations() ) {
			applyAssociationOperation( key, action, associationContext );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return false;
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		int nextValue = neo4jSequenceGenerator.nextValue( key, increment, initialValue );
		value.initialize( nextValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return Neo4jTypeConverter.INSTANCE.convert( type );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key != null ) {
			neo4jCRUD.remove( key );
		}
	}

	private void applyAssociationOperation(AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( key, operation );
			break;
		case PUT_NULL:
			removeAssociationOperation( key, operation );
			break;
		case REMOVE:
			removeAssociationOperation( key, operation );
			break;
		}
	}

	private void putAssociationOperation(AssociationKey associationKey, AssociationOperation action) {
		RowKey rowKey = action.getKey();
		Relationship relationship = neo4jCRUD.findRelationship( associationKey, rowKey );
		if ( relationship == null ) {
			relationship = createRelationship( associationKey, rowKey );
		}
		applyTupleOperations( relationship, action.getValue().getOperations() );
	}

	private void removeAssociationOperation(AssociationKey associationKey, AssociationOperation action) {
		neo4jCRUD.remove( associationKey, action.getKey() );
	}

	private Relationship createRelationship(AssociationKey associationKey, RowKey rowKey) {
		Relationship relationship;
		ResourceIterator<Relationship> relationshipIterator = neo4jCRUD.findRelationship( rowKey );
		if ( relationshipIterator.hasNext() ) {
			// The inverse association exists
			Relationship inverseRelationship = relationshipIterator.next();
			Node node = neo4jCRUD.findNode( associationKey.getEntityKey() );
			inverseRelationship = replaceEndNode( inverseRelationship, node );
			relationship = node.createRelationshipTo( inverseRelationship.getStartNode(), relationshipType( associationKey ) );
		}
		else {
			// No association of this type has been created yet
			relationship = neo4jCRUD.createRelationshipUnlessExists( associationKey, rowKey );
		}
		relationshipIterator.close();
		return relationship;
	}

	private Relationship replaceEndNode(Relationship relationship, Node newNode) {
		// It's not possible to replace a node in an existing relationship therefore we need to create a new
		// relationship and
		// delete the old one
		Node endNode = relationship.getEndNode();
		Node startNode = relationship.getStartNode();
		Relationship newRelationship = startNode.createRelationshipTo( newNode, relationship.getType() );
		copyProperties( relationship, newRelationship );
		relationship.delete();
		endNode.delete();
		return newRelationship;
	}

	private void copyProperties(PropertyContainer from, PropertyContainer to) {
		for ( String key : from.getPropertyKeys() ) {
			to.setProperty( key, from.getProperty( key ) );
		}
	}

	private void applyTupleOperations(PropertyContainer node, Set<TupleOperation> operations) {
		for ( TupleOperation operation : operations ) {
			applyOperation( node, operation );
		}
	}

	private void applyOperation(PropertyContainer node, TupleOperation operation) {
		switch ( operation.getType() ) {
		case PUT:
			putTupleOperation( node, operation );
			break;
		case PUT_NULL:
			removeTupleOperation( node, operation );
			break;
		case REMOVE:
			removeTupleOperation( node, operation );
			break;
		}
	}

	private void removeTupleOperation(PropertyContainer node, TupleOperation operation) {
		if ( node.hasProperty( operation.getColumn() ) ) {
			node.removeProperty( operation.getColumn() );
		}
	}

	private void putTupleOperation(PropertyContainer node, TupleOperation operation) {
		node.setProperty( operation.getColumn(), operation.getValue() );
	}
	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			ResourceIterator<Node> queryNodes = neo4jCRUD.findNodes( entityKeyMetadata.getTable() );
			try {
				while ( queryNodes.hasNext() ) {
					Node next = queryNodes.next();
					Tuple tuple = createTuple( next );
					consumer.consume( tuple );
				}
			}
			finally {
				queryNodes.close();
			}
		}
	}

	@Override
	public TupleIterator executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		String sql = customQuery.getSQL();
		ExecutionResult result = neo4jCRUD.executeQuery( sql );
		return new Neo4jResultsCursor( result, metadatas[0] );
	}

	private static class Neo4jResultsCursor implements TupleIterator {

		private final EntityKeyMetadata metadata;
		private final ResourceIterator<Map<String, Object>> iterator;

		public Neo4jResultsCursor(ExecutionResult result, EntityKeyMetadata metadata) {
			this.iterator = result.iterator();
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Tuple next() {
			Map<String, Object> next = iterator.next();
			if ( metadata != null ) {
				return createTuple( (Node) next.values().iterator().next() );
			}
			return new Tuple( new MapTupleSnapshot( next ) );
		}

		@Override
		public void remove() {
			iterator.remove();
		}

		@Override
		public void close() throws IOException {
			iterator.close();
		}
	}
}
