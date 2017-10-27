/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.neo4j.HttpNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Davide D'Alto
 */
class HttpNeo4jTestHelperDelegate implements Neo4jTestHelperDelegate {

	public static final HttpNeo4jTestHelperDelegate INSTANCE = new HttpNeo4jTestHelperDelegate();

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private HttpNeo4jTestHelperDelegate() {
	}

	@Override
	public long getNumberOfEntities(Session session, DatastoreProvider provider) {
		HttpNeo4jClient remoteNeo4j = createClient( provider );
		Statement statement = new Statement( ENTITY_COUNT_QUERY );
		statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		return readCountFromResponse( session, remoteNeo4j, statement );
	}

	@Override
	public long getNumberOfAssociations(Session session, DatastoreProvider provider) {
		HttpNeo4jClient remoteNeo4j = createClient( provider );
		Statement statement = new Statement( ASSOCIATION_COUNT_QUERY );
		statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		return readCountFromResponse( session, remoteNeo4j, statement );
	}

	private HttpNeo4jClient createClient(DatastoreProvider provider) {
		HttpNeo4jClient remoteNeo4j = ( (HttpNeo4jDatastoreProvider) provider ).getClient();
		return remoteNeo4j;
	}

	@Override
	public GridDialect getDialect(DatastoreProvider datastoreProvider) {
		return new HttpNeo4jDialect( (HttpNeo4jDatastoreProvider) datastoreProvider );
	}

	@Override
	public Graph.Node findNode(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		String nodeAsCypher = node.toCypher();
		String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();

		Statements statements = new Statements();
		statements.addStatement( query, node.getParams() );

		HttpNeo4jClient client = createClient( datastoreProvider );
		StatementsResponse response = client.executeQueriesInNewTransaction( statements );
		validate( response );

		List<Row> data = response.getResults().get( 0 ).getData();
		if ( data.isEmpty() ) {
			return null;
		}
		List<Node> nodes = data.get( 0 ).getGraph().getNodes();

		if ( nodes.size() > 1 ) {
			throw new HibernateException( "Unique result expected" );
		}

		Graph.Node nodeFound = nodes.get( 0 );
		return nodeFound;
	}

	@Override
	public Map<String, Object> findProperties(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		Graph.Node result = findNode( datastoreProvider, node );
		return result.getProperties();
	}

	@Override
	public Object findRelationshipStartNode(DatastoreProvider datastoreProvider, RelationshipsChainForGraphAssertions relationship) {
		HttpNeo4jClient client = createClient( datastoreProvider );
		String relationshipAsCypher = relationship.toCypher();
		NodeForGraphAssertions node = relationship.getStart();
		String query = "MATCH " + relationshipAsCypher + " RETURN " + node.getAlias();

		Statements statements = new Statements();
		statements.addStatement( query, relationship.getParams() );
		StatementsResponse response = client.executeQueriesInNewTransaction( statements );
		validate( response );
		List<Row> data = response.getResults().get( 0 ).getData();
		if ( data.isEmpty() ) {
			return null;
		}
		List<Node> nodes = data.get( 0 ).getGraph().getNodes();
		if ( nodes.size() > 1 ) {
			throw new NotUniqueException();
		}
		return nodes.get( 0 );
	}

	private long readCountFromResponse(Session session, HttpNeo4jClient remoteNeo4j, Statement statement) {
		Statements statements = new Statements();
		statements.addStatement( statement );
		StatementsResponse response = null;
		if ( session != null ) {
			Long transactionId = transactionId( session );
			if ( transactionId != null ) {
				response = remoteNeo4j.executeQueriesInOpenTransaction( transactionId, statements );
			}
			else {
				// Transaction rollbacked or committed
				response = remoteNeo4j.executeQueriesInNewTransaction( statements );
			}
		}
		else {
			response = remoteNeo4j.executeQueriesInNewTransaction( statements );
		}
		return ( (Integer) response.getResults().get( 0 ).getData().get( 0 ).getRow().get( 0 ) ).longValue();
	}

	@Override
	public void deleteAllElements(DatastoreProvider datastoreProvider) {
		HttpNeo4jDatastoreProvider remoteProvider = (HttpNeo4jDatastoreProvider) datastoreProvider;
		Statements statements = new Statements();

		statements.addStatement( DELETE_ALL );
		( (HttpNeo4jClient) remoteProvider.getClient() ).executeQueriesInNewTransaction( statements );
	}

	@Override
	public void dropDatabase(DatastoreProvider datastoreProvider) {
		deleteAllElements( datastoreProvider );
	}

	@Override
	public void executeCypherQuery(DatastoreProvider datastoreProvider, String query, Map<String, Object> parameters) {
			HttpNeo4jDatastoreProvider provider = (HttpNeo4jDatastoreProvider) datastoreProvider;
			Statements statements = new Statements();
			statements.addStatement( query, parameters );
			StatementsResponse statementsResponse = ( (HttpNeo4jClient) provider.getClient() ).executeQueriesInNewTransaction( statements );
			validate( statementsResponse );
	}

	private void validate(StatementsResponse readEntity) {
		if ( !readEntity.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = readEntity.getErrors().get( 0 );
			throw log.nativeQueryException( errorResponse.getCode(), errorResponse.getMessage(), null );
		}
	}

	@Override
	public Long executeCountQuery(DatastoreProvider datastoreProvider, String queryString) {
		Statement statement = new Statement( queryString );
		statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
		Statements statements = new Statements();
		statements.addStatement( statement );

		HttpNeo4jDatastoreProvider remoteProvider = (HttpNeo4jDatastoreProvider) datastoreProvider;
		StatementsResponse response = ( (HttpNeo4jClient) remoteProvider.getClient() ).executeQueriesInNewTransaction( statements );
		Object count = response.getResults().get( 0 ).getData().get( 0 ).getRow().get( 0 );
		return Long.valueOf( count.toString() );
	}

	private Long transactionId(Session session) {
		IdentifiableDriver driver = (IdentifiableDriver) ( ( (SessionImplementor) session ).getTransactionCoordinator().getTransactionDriverControl() );
		if ( session.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			return null;
		}
		Long transactionId = (Long) driver.getTransactionId();
		return transactionId;
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RowStatementsResponse {

		private List<RowStatementsResult> results;

		private List<ErrorResponse> errors;

		public List<RowStatementsResult> getResults() {
			return results;
		}

		public void setResults(List<RowStatementsResult> results) {
			this.results = results;
		}

		public List<ErrorResponse> getErrors() {
			return errors;
		}

		public void setErrors(List<ErrorResponse> errors) {
			this.errors = errors;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RowStatementsResult {

		private List<String> columns;

		private List<RowArray> data;

		public List<String> getColumns() {
			return columns;
		}

		public void setColumns(List<String> columns) {
			this.columns = columns;
		}

		public List<RowArray> getData() {
			return data;
		}

		public void setData(List<RowArray> data) {
			this.data = data;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class RowArray {

		private List<Object> row;

		public List<Object> getRow() {
			return row;
		}

		public void setRow(List<Object> row) {
			this.row = row;
		}
	}
}
