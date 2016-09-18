/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.BaseNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Relationship;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.util.impl.ArrayHelper;

/**
 * @author Davide D'Alto
 */
public class HttpNeo4jAssociationQueries extends BaseNeo4jAssociationQueries {

	public HttpNeo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		super( ownerEntityKeyMetadata, associationKeyMetadata );
	}

	public void removeAssociation(HttpNeo4jClient dataBase, Long txId, AssociationKey associationKey) {
		executeQuery( dataBase, txId, removeAssociationQuery, params( associationKey.getEntityKey().getColumnValues() ) );
	}

	public Relationship findRelationship(HttpNeo4jClient dataBase, Long txId, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		Graph result = executeQuery( dataBase, txId, findRelationshipQuery, params( queryValues ) );
		if ( result != null ) {
			if ( result.getRelationships().size() > 0 ) {
				return result.getRelationships().get( 0 );
			}
		}
		return null;
	}

	public Relationship createRelationshipForEmbeddedAssociation(HttpNeo4jClient executionEngine, Long txId, AssociationKey associationKey, EntityKey embeddedKey,
			Object[] relationshipProperties) {
		String query = initCreateEmbeddedAssociationQuery( associationKey, embeddedKey );
		Object[] queryValues = createRelationshipForEmbeddedQueryValues( associationKey, embeddedKey, relationshipProperties );
		Map<String, Object> params = params( queryValues );
		Graph result = executeQuery( executionEngine, txId, query, params );
		return result.getRelationships().get( 0 );
	}

	public Relationship createRelationship(HttpNeo4jClient dataBase, Long txId, Object[] ownerKeyValues, Object[] targetKeyValues, Object[] relationshipProperties) {
		Object[] concat = ArrayHelper.concat( Arrays.asList( ownerKeyValues, targetKeyValues, relationshipProperties ) );
		Map<String, Object> params = params( concat );
		Graph result = executeQuery( dataBase, txId, createRelationshipQuery, params );
		return result.getRelationships().get( 0 );
	}

	public void removeAssociationRow(HttpNeo4jClient database, Long txId, AssociationKey associationKey, RowKey rowKey) {
		Object[] relationshipValues = relationshipValues( associationKey, rowKey );
		Object[] queryValues = ArrayHelper.concat( associationKey.getEntityKey().getColumnValues(), relationshipValues );
		executeQuery( database, txId, removeAssociationRowQuery, params( queryValues ) );
	}

	private static Graph executeQuery(HttpNeo4jClient executionEngine, Long txId, String query, Map<String, Object> properties) {
		Statements statements = new Statements();
		statements.addStatement( query, properties );
		StatementsResponse statementsResponse = executionEngine.executeQueriesInOpenTransaction( txId, statements );
		validate( statementsResponse );
		List<StatementResult> results = statementsResponse.getResults();
		if ( results == null || results.isEmpty() ) {
			return null;
		}
		if ( results.get( 0 ).getData().isEmpty() ) {
			return null;
		}
		Row row = row( results );
		return row.getGraph();
	}

	public static Row row(List<StatementResult> results) {
		Row row = results.get( 0 ).getData().get( 0 );
		return row;
	}

	private static void validate(StatementsResponse readEntity) {
		if ( !readEntity.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = readEntity.getErrors().get( 0 );
			throw new HibernateException( String.valueOf( errorResponse ) );
		}
	}
}
