/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.neo4j.dialect.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jIndexManager;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
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
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Abstracts Hibernate OGM from Neo4j.
 * <p>
 * A {@link Tuple} is saved as a {@link Node} where the columns are converted into properties of the node.<br>
 * An {@link Association} is converted into a {@link Relationship} identified by the {@link AssociationKey} and the
 * {@link RowKey}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jDialect implements GridDialect {

	/**
	 * Contains the name of the property with the table name.
	 */
	public static final String TABLE_PROPERTY = "_table";

	private final Neo4jDatastoreProvider provider;

	private final Neo4jIndexManager indexer;

	public Neo4jDialect(Neo4jDatastoreProvider provider) {
		this.provider = provider;
		this.indexer = new Neo4jIndexManager( provider );
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Neo4j GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext context) {
		Node entityNode = findNode( key );
		if ( entityNode == null ) {
			return null;
		}
		return createTuple( entityNode );
	}

	private Tuple createTuple(Node entityNode) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple();
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Node node = createNodeUnlessExists( key );
		applyTupleOperations( node, tuple.getOperations() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		Node entityNode = findNode( key );
		if ( entityNode != null ) {
			removeRelationships( entityNode );
			removeNode( entityNode );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		Node entityNode = findNode( associationKey.getEntityKey() );
		if ( entityNode == null ) {
			return null;
		}
		return new Association( new Neo4jAssociationSnapshot( entityNode, relationshipType( associationKey ), associationKey ) );
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
		int nextValue = provider.nextValue( key, increment, initialValue );
		value.initialize( nextValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return Neo4jTypeConverter.INSTANCE.convert( type );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key != null ) {
			Node node = findNode( key.getEntityKey() );
			Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING, relationshipType( key ) );
			for ( Relationship rel : relationships ) {
				removeRelationship( rel );
			}
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
		Relationship relationship = createRelationshipUnlessExists( findNode( associationKey.getEntityKey() ), associationKey, rowKey );
		applyTupleOperations( relationship.getEndNode(), action.getValue().getOperations() );
	}

	private Relationship createRelationshipUnlessExists(Node startNode, AssociationKey associationKey, RowKey rowKey) {
		Relationship relationship = indexer.findRelationship( relationshipType( associationKey ), rowKey );
		if ( relationship == null ) {
			return createRelationship( startNode, associationKey, rowKey );
		}
		return relationship;
	}

	private Node findNode(EntityKey entityKey) {
		return indexer.findNode( entityKey );
	}

	private void removeAssociationOperation(AssociationKey associationKey, AssociationOperation action) {
		RowKey rowKey = action.getKey();
		Relationship relationship = indexer.findRelationship( relationshipType( associationKey ), rowKey );
		removeRelationship( relationship );
	}

	private void removeRelationship(Relationship relationship) {
		if ( relationship != null ) {
			indexer.remove( relationship );
			relationship.delete();
		}
	}

	private void applyTupleOperations(Node node, Set<TupleOperation> operations) {
		for ( TupleOperation operation : operations ) {
			applyOperation( node, operation );
		}
	}

	private void applyOperation(Node node, TupleOperation operation) {
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

	private void removeTupleOperation(Node node, TupleOperation operation) {
		if ( node.hasProperty( operation.getColumn() ) ) {
			node.removeProperty( operation.getColumn() );
		}
	}

	private void putTupleOperation(Node node, TupleOperation operation) {
		node.setProperty( operation.getColumn(), operation.getValue() );
	}

	private Node createNodeUnlessExists(EntityKey key) {
		Node node = findNode( key );
		if ( node == null ) {
			node = createNode( key );
		}
		return node;
	}

	private Node createNode(EntityKey key) {
		Node node = provider.createNode();
		node.setProperty( TABLE_PROPERTY, key.getTable() );
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			node.setProperty( key.getColumnNames()[i], key.getColumnValues()[i] );
		}
		indexer.index( node, key );
		return node;
	}

	private void removeNode(Node entityNode) {
		removeRelationships( entityNode );
		indexer.remove( entityNode );
		entityNode.delete();
	}

	private Relationship createRelationship(Node startNode, AssociationKey associationKey, RowKey rowKey) {
		Relationship relationship = startNode.createRelationshipTo( provider.createNode(), relationshipType( associationKey ) );
		for ( int i = 0; i < rowKey.getColumnNames().length; i++ ) {
			Object value = rowKey.getColumnValues()[i];
			if ( value != null ) {
				relationship.setProperty( rowKey.getColumnNames()[i], value );
			}
		}
		indexer.index( relationship );
		return relationship;
	}

	private RelationshipType relationshipType(AssociationKey associationKey) {
		StringBuilder builder = new StringBuilder( associationKey.getEntityKey().getTable() );
		builder.append( ":" );
		builder.append( associationKey.getCollectionRole() );
		return DynamicRelationshipType.withName( builder.toString() );
	}

	private void removeRelationships(Node node) {
		if ( node != null ) {
			for ( Relationship rel : node.getRelationships() ) {
				removeRelationship( rel );
			}
		}
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			IndexHits<Node> queryNodes = indexer.findNodes( entityKeyMetadata.getTable() );
			try {
				for ( Node node : queryNodes ) {
					Tuple tuple = createTuple( node );
					consumer.consume( tuple );
				}
			}
			finally {
				queryNodes.close();
			}
		}
	}

	@Override
	public TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas) {
		throw new UnsupportedOperationException( "Native queries not suported for Neo4j" );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}
}
