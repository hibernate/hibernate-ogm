/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.escapeIdentifier;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.util.impl.ArrayHelper;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

/**
 * The {@link Neo4jDialect} will use this queries to update the associations on Neo4j
 *
 * @author Davide D'Alto
 */
public class Neo4jAssociationQueries {

	private final String findRelationshipQuery;
	private final String removeAssociationQuery;
	private final String removeAssociationRowQuery;

	public Neo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		this.removeAssociationQuery = initRemoveAssociationQuery( ownerEntityKeyMetadata, associationKeyMetadata );
		this.removeAssociationRowQuery = initRemoveAssociationRowQuery( ownerEntityKeyMetadata, associationKeyMetadata );
		this.findRelationshipQuery = initFindRelationshipQuery( ownerEntityKeyMetadata, associationKeyMetadata );
	}

	/*
	 * Example with target node:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role] -  (t {id: {1}})
	 * RETURN r
	 *
	 * Example with relationship indexes:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role {index: {1}}] -  (t)
	 * RETURN r
	 */
	private String initFindRelationshipQuery(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		int offset = 0;
		StringBuilder queryBuilder = new StringBuilder("MATCH ");
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( ownerEntityKeyMetadata, queryBuilder );
		appendProperties( ownerEntityKeyMetadata, queryBuilder );
		queryBuilder.append( ") - " );
		queryBuilder.append( "[r" );
		queryBuilder.append( ":" );
		appendRelationshipType( queryBuilder, associationKeyMetadata );
		offset = ownerEntityKeyMetadata.getColumnNames().length;
		if ( associationKeyMetadata.getRowKeyIndexColumnNames().length > 0 ) {
			appendProperties( queryBuilder, associationKeyMetadata.getRowKeyIndexColumnNames(), offset );
			queryBuilder.append( "] - (t" );
		}
		else {
			queryBuilder.append( "] - (t" );
			appendProperties( queryBuilder, associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getColumnNames(), offset );
		}
		queryBuilder.append( ")" );
		queryBuilder.append( " RETURN r" );
		return queryBuilder.toString();
	}

	/*
	 * Example with association:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role] - ()
	 * DELETE r
	 *
	 * Example with embedded:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role] - (e:EMBEDDED)
	 * DELETE r, e
	 */
	private String initRemoveAssociationQuery(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder("MATCH ");
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( ownerEntityKeyMetadata, queryBuilder );
		appendProperties( ownerEntityKeyMetadata, queryBuilder );
		queryBuilder.append( ") - " );
		queryBuilder.append( "[r" );
		queryBuilder.append( ":" );
		appendRelationshipType( queryBuilder, associationKeyMetadata );
		queryBuilder.append( "]" );
		if ( associationKeyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			queryBuilder.append( " - (e:" );
			queryBuilder.append( EMBEDDED );
			queryBuilder.append( ") DELETE r, e" );
		}
		else {
			queryBuilder.append( " - () DELETE r" );
		}
		return queryBuilder.toString();
	}

	/*
	 * Example with association:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role] - (e)
	 * DELETE r
	 *
	 * Example with embedded collection:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role] - (e:EMBEDDED)
	 * DELETE r, e
	 *
	 * Example with indexes:
	 *
	 * MATCH (n:ENTITY:table {id: {0}}) -[r:role {index: {1}}] - (e)
	 * DELETE r
	 */
	private String initRemoveAssociationRowQuery(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		StringBuilder queryBuilder = new StringBuilder("MATCH ");
		queryBuilder.append( "(n:" );
		queryBuilder.append( ENTITY );
		queryBuilder.append( ":" );
		appendLabel( ownerEntityKeyMetadata, queryBuilder );
		appendProperties( ownerEntityKeyMetadata, queryBuilder );
		queryBuilder.append( ") - " );
		queryBuilder.append( "[r" );
		queryBuilder.append( ":" );
		appendRelationshipType( queryBuilder, associationKeyMetadata );
		int offset = ownerEntityKeyMetadata.getColumnNames().length;
		boolean hasIndexColumns = associationKeyMetadata.getRowKeyIndexColumnNames().length > 0;
		if ( hasIndexColumns ) {
			appendProperties( queryBuilder, associationKeyMetadata.getRowKeyIndexColumnNames(), offset );
		}
		queryBuilder.append( "] - (e" );
		if ( associationKeyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			queryBuilder.append( ":" );
			queryBuilder.append( EMBEDDED );
		}
		if ( !hasIndexColumns ) {
			appendProperties( queryBuilder, associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getColumnNames(), offset );
		}
		queryBuilder.append( ")" );
		queryBuilder.append( " DELETE r" );
		if ( associationKeyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			queryBuilder.append( ", e" );
		}
		return queryBuilder.toString();
	}

	/**
	 * Removes the relationship(s) representing the given association. If the association refers to an embedded entity
	 * (collection), the referenced entities are removed as well.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param associationKey represents the association
	 */
	public void removeAssociation(ExecutionEngine executionEngine, AssociationKey associationKey) {
		executionEngine.execute( removeAssociationQuery, params( associationKey.getEntityKey().getColumnValues() ) );
	}

	private void appendProperties(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		appendProperties( queryBuilder, entityKeyMetadata.getColumnNames(), 0 );
	}

	private void appendProperties(StringBuilder queryBuilder, String[] columnNames, int offset) {
		if ( columnNames.length > 0 ) {
			queryBuilder.append( " {" );
			for ( int i = 0; i < columnNames.length; i++ ) {
				escapeIdentifier( queryBuilder, columnNames[i] );
				queryBuilder.append( ": {" );
				queryBuilder.append( offset + i );
				queryBuilder.append( "}" );
				if ( i < columnNames.length - 1 ) {
					queryBuilder.append( ", " );
				}
			}
			queryBuilder.append( "}" );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T singleResult(ExecutionResult result) {
		ResourceIterator<Map<String, Object>> iterator = result.iterator();
		try {
			if ( iterator.hasNext() ) {
				return (T) iterator.next().values().iterator().next();
			}
			return null;
		}
		finally {
			iterator.close();
		}
	}

	private Map<String, Object> params(Object[] columnValues) {
		return params( columnValues, 0 );
	}

	private Map<String, Object> params(Object[] columnValues, int offset) {
		Map<String, Object> params = new HashMap<String, Object>( columnValues.length );
		for ( int i = 0; i < columnValues.length; i++ ) {
			params.put( String.valueOf( offset + i ), columnValues[i] );
		}
		return params;
	}

	/**
	 * Returns the relationship corresponding to the {@link AssociationKey} and {@link RowKey}.
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param associationKey represents the association
	 * @param rowKey represents a row in an association
	 * @return the corresponding relationship
	 */
	public Relationship findRelationship(ExecutionEngine executionEngine, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues;
		if ( associationKey.getMetadata().getRowKeyIndexColumnNames().length > 0 ) {
			int length = associationKey.getMetadata().getRowKeyIndexColumnNames().length;
			relationshipValues = new Object[length];
			String[] indexColumnNames = associationKey.getMetadata().getRowKeyIndexColumnNames();
			for ( int i = 0; i < indexColumnNames.length; i++ ) {
				for ( int j = 0; j < rowKey.getColumnNames().length; j++ ) {
					if ( indexColumnNames[i].equals( rowKey.getColumnNames()[j] ) ) {
						relationshipValues[i] = rowKey.getColumnValues()[j];
					}
				}
			}
		}
		else {
			relationshipValues = getEntityKey( associationKey, rowKey ).getColumnValues();
		}
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		ExecutionResult result = executionEngine.execute( findRelationshipQuery, params( queryValues ) );
		return singleResult( result );
	}

	/**
	 * Remove an association row
	 *
	 * @param executionEngine the {@link ExecutionEngine} used to run the query
	 * @param associationKey represents the association
	 * @param rowKey represents a row in an association
	 */
	public void removeAssociationRow(ExecutionEngine executionEngine, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues;
		if ( associationKey.getMetadata().getRowKeyIndexColumnNames().length > 0 ) {
			int length = associationKey.getMetadata().getRowKeyIndexColumnNames().length;
			relationshipValues = new Object[length];
			String[] indexColumnNames = associationKey.getMetadata().getRowKeyIndexColumnNames();
			for ( int i = 0; i < indexColumnNames.length; i++ ) {
				for ( int j = 0; j < rowKey.getColumnNames().length; j++ ) {
					if ( indexColumnNames[i].equals( rowKey.getColumnNames()[j] ) ) {
						relationshipValues[i] = rowKey.getColumnValues()[j];
					}
				}
			}
		}
		else {
			relationshipValues = getEntityKey( associationKey, rowKey ).getColumnValues();
		}
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		executionEngine.execute( removeAssociationRowQuery, params( queryValues ) );
	}

	/**
	 * Returns the entity key on the other side of association row represented by the given row key.
	 * <p>
	 * <b>Note:</b> May only be invoked if the row key actually contains all the columns making up that entity key.
	 * Specifically, it may <b>not</b> be invoked if the association has index columns (maps, ordered collections), as
	 * the entity key columns will not be part of the row key in this case.
	 */
	private EntityKey getEntityKey(AssociationKey associationKey, RowKey rowKey) {
		String[] associationKeyColumns = associationKey.getMetadata().getAssociatedEntityKeyMetadata().getAssociationKeyColumns();
		Object[] columnValues = new Object[associationKeyColumns.length];
		int i = 0;

		for ( String associationKeyColumn : associationKeyColumns ) {
			columnValues[i] = rowKey.getColumnValue( associationKeyColumn );
			i++;
		}

		EntityKeyMetadata entityKeyMetadata = associationKey.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
		return new EntityKey( entityKeyMetadata, columnValues );
	}

	private void appendLabel(EntityKeyMetadata entityKeyMetadata, StringBuilder queryBuilder) {
		escapeIdentifier( queryBuilder, entityKeyMetadata.getTable() );
	}

	private void appendRelationshipType(StringBuilder queryBuilder, AssociationKeyMetadata associationKeyMetadata) {
		escapeIdentifier( queryBuilder, associationKeyMetadata.getCollectionRole() );
	}
}
