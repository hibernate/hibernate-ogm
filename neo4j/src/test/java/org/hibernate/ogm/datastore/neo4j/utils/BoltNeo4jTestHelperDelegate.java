/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.datastore.neo4j.BoltNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.types.Node;

/**
 * @author Davide D'Alto
 */
class BoltNeo4jTestHelperDelegate implements Neo4jTestHelperDelegate {

	public static final BoltNeo4jTestHelperDelegate INSTANCE = new BoltNeo4jTestHelperDelegate();

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private BoltNeo4jTestHelperDelegate() {
	}

	@Override
	public long getNumberOfEntities(Session session, DatastoreProvider provider) {
		BoltNeo4jClient client = createClient( provider );
		return readCountFromResponse( session, client.getDriver(), ENTITY_COUNT_QUERY );
	}

	@Override
	public long getNumberOfAssociations(Session session, DatastoreProvider provider) {
		BoltNeo4jClient client = createClient( provider );
		return readCountFromResponse( session, client.getDriver(), ASSOCIATION_COUNT_QUERY );
	}

	private long readCountFromResponse(Session session, Driver driver, String query) {
		StatementResult response = null;
		if ( session != null ) {
			Transaction transactionId = transactionId( session );
			if ( transactionId != null ) {
				response = transactionId.run( query );
			}
			else {
				// Transaction rollbacked or committed
				try ( org.neo4j.driver.v1.Session neo4jSession = driver.session() ) {
					response = neo4jSession.run( query );
				}
			}
		}
		else {
			try ( org.neo4j.driver.v1.Session neo4jSession = driver.session() ) {
				response = neo4jSession.run( query );
			}
		}
		return response.single().get( 0 ).asInt();
	}

	private Transaction transactionId(Session session) {
		IdentifiableDriver driver = (IdentifiableDriver) ( ( (SessionImplementor) session ).getTransactionCoordinator().getTransactionDriverControl() );
		if ( session.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			return null;
		}
		return (Transaction) driver.getTransactionId();
	}

	private BoltNeo4jClient createClient(DatastoreProvider provider) {
		BoltNeo4jClient client = ( (BoltNeo4jDatastoreProvider) provider ).getClient();
		return client;
	}

	@Override
	public GridDialect getDialect(DatastoreProvider datastoreProvider) {
		return new BoltNeo4jDialect( (BoltNeo4jDatastoreProvider) datastoreProvider );
	}

	@Override
	public Node findNode(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		String nodeAsCypher = node.toCypher();
		String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();
		Driver driver = driver( datastoreProvider );

		List<Record> records = null;
		try ( org.neo4j.driver.v1.Session neo4jSession = driver.session() ) {
			Statement statement = new Statement( query, node.getParams() );
			StatementResult result = neo4jSession.run( statement );
			validate( result );
			records = result.list();
		}

		if ( records.isEmpty() ) {
			return null;
		}
		if ( records.size() > 1 ) {
			throw new NotUniqueException();
		}

		Node nodeFound = records.get( 0 ).get( node.getAlias() ).asNode();
		return nodeFound;
	}

	@Override
	public Map<String, Object> findProperties(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		Node result = findNode( datastoreProvider, node );
		return result.asMap();
	}

	@Override
	public Object findRelationshipStartNode(DatastoreProvider datastoreProvider, RelationshipsChainForGraphAssertions relationship) {
		String relationshipAsCypher = relationship.toCypher();
		NodeForGraphAssertions node = relationship.getStart();
		String query = "MATCH " + relationshipAsCypher + " RETURN " + node.getAlias();
		List<Record> records = null;
		Driver driver = driver( datastoreProvider );

		try ( org.neo4j.driver.v1.Session neo4jSession = driver.session() ) {
			Statement statement = new Statement( query, relationship.getParams() );
			StatementResult result = neo4jSession.run( statement );
			validate( result );
			records = result.list();
		}

		if ( records.isEmpty() ) {
			return null;
		}
		if ( records.size() > 1 ) {
			throw new NotUniqueException();
		}

		Node nodeFound = records.get( 0 ).get( node.getAlias() ).asNode();
		return nodeFound;
	}

	@Override
	public void deleteAllElements(DatastoreProvider datastoreProvider) {
		Driver driver = driver( datastoreProvider );
		try ( org.neo4j.driver.v1.Session session = driver.session() ) {
			session.run( DELETE_ALL );
		}
	}

	private Driver driver(DatastoreProvider datastoreProvider) {
		BoltNeo4jDatastoreProvider boltProvider = (BoltNeo4jDatastoreProvider) datastoreProvider;
		Driver driver = boltProvider.getClient().getDriver();
		return driver;
	}

	@Override
	public void dropDatabase(DatastoreProvider datastoreProvider) {
		deleteAllElements( datastoreProvider );
	}

	@Override
	public void executeCypherQuery(DatastoreProvider datastoreProvider, String query, Map<String, Object> parameters) {
		Driver driver = driver( datastoreProvider );
		try ( org.neo4j.driver.v1.Session session = driver.session() ) {
			StatementResult result = session.run( query, parameters );
			validate( result );
		}
	}

	private void validate(StatementResult result) {
		try {
			result.hasNext();
		}
		catch (ClientException e) {
			throw log.nativeQueryException( e.code(), e.getMessage(), null );
		}

	}

	@Override
	public Long executeCountQuery(DatastoreProvider datastoreProvider, String queryString) {
		Driver driver = driver( datastoreProvider );
		StatementResult result = driver.session().run( queryString );
		return result.single().get( 0 ).asLong();
	}
}
