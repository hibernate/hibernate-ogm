/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutionException;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan Clustered Counter feature.
 * Used by the dialect to implement a reliable ID generator.
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */
public abstract class ClusteredCounterHandler {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );
	protected final EmbeddedCacheManager cacheManager;

	public ClusteredCounterHandler(EmbeddedCacheManager cacheManager) {
		this.cacheManager = cacheManager;
		validate();
	}

	private void validate() {
		if ( cacheManager.getTransport() == null ) {
			throw LOG.counterCannotBeCreatedForLocalCaches();
		}
	}

	/**
	 * Create a counter if one is not defined already, otherwise return the existing one.
	 *
	 * @param counterName unique name for the counter
	 * @param initialValue initial value for the counter
	 * @return a {@link StrongCounter}
	 */
	protected StrongCounter getCounterOrCreateIt(String counterName, int initialValue) {
		CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager( cacheManager );
		if ( !counterManager.isDefined( counterName ) ) {
			LOG.tracef( "Counter %s is not defined, creating it", counterName );

			// global configuration is mandatory in order to define
			// a new clustered counter with persistent storage
			validateGlobalConfiguration();

			counterManager.defineCounter( counterName,
				CounterConfiguration.builder(
					CounterType.UNBOUNDED_STRONG )
						.initialValue( initialValue )
						.storage( Storage.PERSISTENT )
						.build() );

			LOG.tracef( "Counter %s is not defined, creating it", counterName );
		}

		StrongCounter strongCounter = counterManager.getStrongCounter( counterName );
		return strongCounter;
	}

	private void validateGlobalConfiguration() {
		boolean globalConfigIsEnabled = cacheManager.getGlobalComponentRegistry()
			.getGlobalConfiguration().globalState()
			.enabled();

		if ( !globalConfigIsEnabled ) {
			throw LOG.counterCannotBeCreatedWithoutGlobalConfiguration();
		}
	}

	protected Number nextValue(NextValueRequest request, StrongCounter strongCounter) {
		try {
			Long newValue = strongCounter.addAndGet( request.getIncrement() ).get();
			return newValue - request.getIncrement();
		}
		catch (ExecutionException | InterruptedException e) {
			throw new HibernateException( "Interrupting Operation " + e.getMessage(), e );
		}
	}

	public abstract Number nextValue(NextValueRequest request);
}
