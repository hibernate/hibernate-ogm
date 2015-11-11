/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.dialect.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.AssociationQueries;
import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Relationship;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.util.impl.ArrayHelper;

/**
 * @author Davide D'Alto
 */
public class RemoteNeo4jAssociationQueries extends AssociationQueries {

	public RemoteNeo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		super( ownerEntityKeyMetadata, associationKeyMetadata );
	}

	public void removeAssociation(Neo4jClient dataBase, AssociationKey associationKey) {
		executeQuery( dataBase, removeAssociationQuery, params( associationKey.getEntityKey().getColumnValues() ) );
	}

	public Relationship findRelationship(Neo4jClient dataBase, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		List<StatementResult> results = executeQuery( dataBase, findRelationshipQuery, params( queryValues ) );
		if ( results != null ) {
			Row row = results.get( 0 ).getData().get( 0 );
			if ( row.getGraph().getRelationships().size() > 0 ) {
				return row.getGraph().getRelationships().get( 0 );
			}
		}
		return null;
	}

	public Relationship createRelationshipForEmbeddedAssociation(Neo4jClient executionEngine, AssociationKey associationKey, EntityKey embeddedKey,
			Object[] relationshipProperties) {
		String query = initCreateEmbeddedAssociationQuery( associationKey, embeddedKey );
		Object[] queryValues = createRelationshipForEmbeddedQueryValues( associationKey, embeddedKey, relationshipProperties );
		Map<String, Object> params = params( queryValues );
		List<StatementResult> result = executeQuery( executionEngine, query, params );
		return result.get( 0 ).getData().get( 0 ).getGraph().getRelationships().get( 0 );
	}

	public Relationship createRelationship(Neo4jClient dataBase, Object[] ownerKeyValues, Object[] targetKeyValues, Object[] relationshipProperties) {
		Object[] concat = ArrayHelper.concat( Arrays.asList( ownerKeyValues, targetKeyValues, relationshipProperties ) );
		Map<String, Object> params = params( concat );
		List<StatementResult> results = executeQuery( dataBase, createRelationshipQuery, params );
		return results.get( 0 ).getData().get( 0 ).getGraph().getRelationships().get( 0 );
	}

	public void removeAssociationRow(Neo4jClient database, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		executeQuery( database, removeAssociationRowQuery, params( queryValues ) );
	}

	private static List<StatementResult> executeQuery(Neo4jClient executionEngine, String query, Map<String, Object> properties) {
		Statements statements = new Statements();
		statements.addStatement( query, properties );
		StatementsResponse statementsResponse = executionEngine.executeQueriesInOpenTransaction( statements );
		validate( statementsResponse );
		List<StatementResult> results = statementsResponse.getResults();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		if ( results.get( 0 ).getData().isEmpty() ) {
			return null;
		}
		return results;
	}

	private static void validate(StatementsResponse readEntity) {
		if ( !readEntity.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = readEntity.getErrors().get( 0 );
			throw new HibernateException( String.valueOf( errorResponse ) );
		}
	}
}
