/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.GridDialectTestHelper;
import org.hibernate.ogm.utils.TestHelper;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jTestHelper implements GridDialectTestHelper {

	static {
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
	public Map<String, String> getEnvironmentProperties() {
		Map<String, String> envProps = new HashMap<String, String>( 2 );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PASSWORD, envProps );

		// The configuration file overrides the environment properties
		envProps.putAll( PropertiesReader.getHibernateProperties() );
		envProps.put( Neo4jProperties.DATABASE_PATH, EmbeddedNeo4jTestHelperDelegate.dbLocation() );
		return envProps;
	}

	private void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

	private static DatastoreProvider getDatastoreProvider(SessionFactory sessionFactory) {
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

	private static GridDialect getDialect(SessionFactory sessionFactory) {
		GridDialect dialect = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( GridDialect.class );
		return dialect;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
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

	@Override
	public void prepareDatabase(SessionFactory sessionFactory) {
	}
}
