/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.ogm.datastore.neo4j.BoltNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.transaction.impl.BoltNeo4jTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jConfiguration;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.neo4j.driver.v1.Driver;

/**
 * @author Davide D'Alto
 */
public class BoltNeo4jDatastoreProvider extends RemoteNeo4jDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final String BOLT_PROTOCOL = "bolt";

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private BoltNeo4jClient client;

	private BoltNeo4jSequenceGenerator sequenceGenerator;

	private JndiService jndiService;

	public BoltNeo4jDatastoreProvider() {
		super( BOLT_PROTOCOL, RemoteNeo4jConfiguration.DEFAULT_BOLT_PORT );
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new BoltNeo4jTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return BoltNeo4jSchemaDefiner.class;
	}

	@Override
	public void start() {
		if ( client != null ) {
			return;
		}

		if ( configuration.getNativeClientResource() == null ) {
			createClient();
		}
		else {
			lookupClient();
		}
		this.sequenceGenerator = new BoltNeo4jSequenceGenerator( client, getSequenceCacheMaxSize() );
	}

	private void createClient() {
		try {
			this.client = new BoltNeo4jClient( getDatabaseIdentifier(), configuration );
		}
		catch (HibernateException e) {
			// Wrap HibernateException in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	private void lookupClient() {
		try {
			log.tracef( "Retrieving MongoDatabase from JNDI at %1$s", configuration.getNativeClientResource() );
			Driver driver = (Driver) jndiService.locate( configuration.getNativeClientResource() );
			this.client = new BoltNeo4jClient( driver );
		}
		catch (RuntimeException e) {
			throw log.errorOnFetchJndiClientProperty( configuration.getNativeClientResource() );
		}

		// clear resources
		this.jndiService = null;
	}

	@Override
	public void stop() {
		try {
			if ( client != null ) {
				client.close();
			}
		}
		finally {
			client = null;
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return BoltNeo4jDialect.class;
	}

	@Override
	public BoltNeo4jSequenceGenerator getSequenceGenerator() {
		return sequenceGenerator;
	}

	public BoltNeo4jClient getClient() {
		return client;
	}
}
