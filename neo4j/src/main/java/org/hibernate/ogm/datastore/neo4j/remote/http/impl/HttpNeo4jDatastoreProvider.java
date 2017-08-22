/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.HttpNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jConfiguration;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatabaseIdentifier;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.http.transaction.impl.HttpNeo4jTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Davide D'Alto
 */
public class HttpNeo4jDatastoreProvider extends RemoteNeo4jDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log logger = LoggerFactory.make( MethodHandles.lookup() );

	private static final String HTTP_PROTOCOL = "http";

	private HttpNeo4jClient remoteNeo4j;

	private HttpNeo4jSequenceGenerator sequenceGenerator;

	public HttpNeo4jDatastoreProvider() {
		super( HTTP_PROTOCOL, RemoteNeo4jConfiguration.DEFAULT_HTTP_PORT );
	}

	@Override
	public void stop() {
		if ( remoteNeo4j != null ) {
			remoteNeo4j.close();
			remoteNeo4j = null;
		}
	}

	@Override
	public void start() {
		if ( remoteNeo4j == null ) {
			try {
				remoteNeo4j = createNeo4jClient( getDatabaseIdentifier(), configuration );
				remoteNeo4j.validateConnection();
				sequenceGenerator = new HttpNeo4jSequenceGenerator( remoteNeo4j, getSequenceCacheMaxSize() );
			}
			catch (HibernateException e) {
				// Wrap HibernateException in a ServiceException to make the stack trace more friendly
				// Otherwise a generic unable to request service is thrown
				throw logger.unableToStartDatastoreProvider( e );
			}
		}
	}

	/**
	 * Creates the {@link HttpNeo4jClient} that it is going to be used to connect to a remote Neo4j server.
	 * <p>
	 * I've created a separate method to allow plugging different implementation if needed.
	 *
	 * @param databaseIdentifier the connection properties to identify a database
	 * @param configuration all the configuration properties
	 * @return a client that can access a Neo4j server
	 */
	public HttpNeo4jClient createNeo4jClient(RemoteNeo4jDatabaseIdentifier databaseIdentifier, RemoteNeo4jConfiguration configuration) {
		return new HttpNeo4jClient( databaseIdentifier, configuration );
	}

	public HttpNeo4jClient getClient() {
		return remoteNeo4j;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return HttpNeo4jDialect.class;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return HttpNeo4jSchemaDefiner.class;
	}

	@Override
	public HttpNeo4jSequenceGenerator getSequenceGenerator() {
		return sequenceGenerator;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new HttpNeo4jTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
