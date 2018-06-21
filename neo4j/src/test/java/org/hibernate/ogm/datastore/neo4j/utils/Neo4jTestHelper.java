/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.index.impl.Neo4jIndexSpec;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jSchemaDefiner;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.ogm.utils.BaseGridDialectTestHelper;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.GridDialectTestHelper;
import org.hibernate.ogm.utils.TestHelper;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jTestHelper extends BaseGridDialectTestHelper implements GridDialectTestHelper {

	private static final String GET_DB_INDEXES_PROCEDURE = "CALL db.indexes;";

	private static final String INDEX_LABEL = "label";
	private static final String INDEX_PROPERTIES = "properties";
	private static final String INDEX_TYPE = "type";
	private static final String INDEX_TYPE_UNIQUE = "node_unique_property";

	static {
		initEnvironment();
	}

	public static void initEnvironment() {
		// Read host, username and password from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String neo4jHost = System.getenv( "NEO4J_HOSTNAME" );
		if ( isNotNull( neo4jHost ) ) {
			System.getProperties().setProperty( OgmProperties.HOST, neo4jHost );
		}
		String neo4jPort = System.getenv( "NEO4J_PORT" );
		if ( isNotNull( neo4jPort ) ) {
			System.getProperties().setProperty( OgmProperties.PORT, neo4jPort );
		}
		String neo4jUsername = System.getenv( "NEO4J_USERNAME" );
		if ( isNotNull( neo4jUsername ) ) {
			System.getProperties().setProperty( OgmProperties.USERNAME, neo4jUsername );
		}
		String neo4jPassword = System.getenv( "NEO4J_PASSWORD" );
		if ( isNotNull( neo4jPassword ) ) {
			System.getProperties().setProperty( OgmProperties.PASSWORD, neo4jPassword );
		}
	}

	private static boolean isNotNull(String neo4jHostName) {
		return neo4jHostName != null && neo4jHostName.length() > 0 && !"null".equals( neo4jHostName );
	}

	@Override
	public long getNumberOfEntities(Session session) {
		DatastoreProvider provider = getDatastoreProvider( session.getSessionFactory() );
		return delegate().getNumberOfEntities( session, provider );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		DatastoreProvider provider = getDatastoreProvider( sessionFactory );
		return delegate().getNumberOfEntities( null, provider );
	}

	@Override
	public long getNumberOfAssociations(Session session) {
		DatastoreProvider provider = getDatastoreProvider( session.getSessionFactory() );
		return delegate().getNumberOfAssociations( session, provider );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		DatastoreProvider provider = getDatastoreProvider( sessionFactory );
		return delegate().getNumberOfAssociations( null, provider );
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		Map<String, Object> tuple = new HashMap<String, Object>();
		GridDialect dialect = getDialect( session.getSessionFactory() );
		TupleContext context = tupleContext( session );
		TupleSnapshot snapshot = dialect.getTuple( key, context ).getSnapshot();
		for ( String column : snapshot.getColumnNames() ) {
			tuple.put( column, snapshot.get( column ) );
		}
		return tuple;
	}

	private TupleContext tupleContext(Session session) {
		return new GridDialectOperationContexts.TupleContextBuilder().transactionContext( session )
				.tupleTypeContext( GridDialectOperationContexts.emptyTupleTypeContext() )
				.buildTupleContext();
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		DatastoreProvider datastoreProvider = getDatastoreProvider( sessionFactory );
		delegate().dropDatabase( datastoreProvider );
	}

	@Override
	public Map<String, String> getAdditionalConfigurationProperties() {
		return Collections.singletonMap( Neo4jProperties.DATABASE_PATH, EmbeddedNeo4jTestHelperDelegate.dbLocation() );
	}

	public static DatastoreProvider getDatastoreProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( EmbeddedNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return EmbeddedNeo4jDatastoreProvider.class.cast( provider );
		}
		if ( HttpNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return HttpNeo4jDatastoreProvider.class.cast( provider );
		}
		if ( BoltNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return BoltNeo4jDatastoreProvider.class.cast( provider );
		}
		throw new RuntimeException( "Not testing with Neo4jDB, cannot extract underlying provider" );
	}

	public static List<Neo4jIndexSpec> getIndexes(DatastoreProvider provider) {
		if ( EmbeddedNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return getEmbeddedNeo4jIndexes( provider );
		}
		if ( HttpNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return getHttpNeo4jIndexes( provider );
		}
		if ( BoltNeo4jDatastoreProvider.class.isInstance( provider ) ) {
			return getBoltNeo4jIndexes( provider );
		}
		return Collections.emptyList();
	}

	private static List<Neo4jIndexSpec> getEmbeddedNeo4jIndexes(DatastoreProvider provider) {
		GraphDatabaseService neo4jDb = EmbeddedNeo4jDatastoreProvider.class.cast( provider ).getDatabase();
		try ( Transaction tx = neo4jDb.beginTx() ) {
			List<Neo4jIndexSpec> indexes = CollectionHelper.toStream( neo4jDb.schema().getIndexes() )
					.map( Neo4jTestHelper::extractNeo4jIndexSpec )
					.collect( Collectors.toList() );
			tx.success();
			return indexes;
		}
	}

	private static List<Neo4jIndexSpec> getHttpNeo4jIndexes(DatastoreProvider provider) {
		HttpNeo4jClient neo4jClient = HttpNeo4jDatastoreProvider.class.cast( provider ).getClient();
		Statements statements = new Statements();
		statements.addStatement( GET_DB_INDEXES_PROCEDURE, Collections.emptyMap(), Statement.AS_ROW );
		StatementsResponse response = neo4jClient.executeQueriesInNewTransaction( statements );
		return response.getResults().stream()
				.flatMap( rs -> rs.getData().stream().map( r -> extractNeo4jIndexSpec( rs.getColumns(), r ) ) )
				.collect( Collectors.toList() );
	}

	private static List<Neo4jIndexSpec> getBoltNeo4jIndexes(DatastoreProvider provider) {
		List<StatementResult> run = BoltNeo4jSchemaDefiner.run( provider, Collections.singletonList( new org.neo4j.driver.v1.Statement( GET_DB_INDEXES_PROCEDURE ) ) );
		return run.stream()
				.flatMap( rs -> rs.list().stream() )
				.map( Neo4jTestHelper::extractNeo4jIndexSpec )
				.collect( Collectors.toList() );
	}

	private static Neo4jIndexSpec extractNeo4jIndexSpec(IndexDefinition i) {
		return new Neo4jIndexSpec( i.getLabel(), CollectionHelper.toList( i.getPropertyKeys() ), i.isConstraintIndex() );
	}

	@SuppressWarnings( "unchecked" )
	private static Neo4jIndexSpec extractNeo4jIndexSpec(List<String> cols, Row row) {
		List<Object> values = row.getRow();
		Label label = Label.label( String.valueOf( values.get( cols.indexOf( INDEX_LABEL ) ) ) );
		List<String> properties = (List<String>) values.get( cols.indexOf( INDEX_PROPERTIES ) );
		boolean uniquePropertyIndex = Objects.equals( values.get( cols.indexOf( INDEX_TYPE ) ), INDEX_TYPE_UNIQUE );
		return new Neo4jIndexSpec( label, properties, uniquePropertyIndex );
	}

	private static Neo4jIndexSpec extractNeo4jIndexSpec(Record r) {
		Label label = Label.label( r.get( INDEX_LABEL ).asString() );
		List<String> properties = r.get( INDEX_PROPERTIES ).asList( Value::asString );
		boolean uniquePropertyIndex = Objects.equals( r.get( INDEX_TYPE ).asString(), INDEX_TYPE_UNIQUE );
		return new Neo4jIndexSpec( label, properties, uniquePropertyIndex );
	}

	private static GridDialect getDialect(SessionFactory sessionFactory) {
		GridDialect dialect = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( GridDialect.class );
		return dialect;
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return Neo4j.class;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return delegate().getDialect( datastoreProvider );
	}

	public static Neo4jTestHelperDelegate delegate() {
		DatastoreProviderType providerType = TestHelper.getCurrentDatastoreProviderType();
		switch ( providerType ) {
			case NEO4J_EMBEDDED:
				return EmbeddedNeo4jTestHelperDelegate.INSTANCE;
			case NEO4J_HTTP:
				return HttpNeo4jTestHelperDelegate.INSTANCE;
			case NEO4J_BOLT:
				return BoltNeo4jTestHelperDelegate.INSTANCE;
			default:
				throw new RuntimeException( "Not testing with Neo4jDB, cannot extract underlying dialect" );
		}
	}
}
