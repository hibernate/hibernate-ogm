/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.NextValueRequest;

import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan Clustered Counter feature.
 * Used by the dialect to implement a reliable ID generator.
 *
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
	 * The method creates the counter only if the counter is not already created.
	 *
	 * @param initialValue initial value for the counter
	 * @param counterManager counterManager service
	 * @param counterName unique name for the counter
	 * @return true if the counter is created by current thread, false otherwise
	 */
	protected boolean defineCounterIfNotExists(int initialValue, CounterManager counterManager, String counterName) {

		// if counter is already present, skipping defining
		if ( counterManager.isDefined( counterName ) ) {

			if ( LOG.isTraceEnabled() ) {
				LOG.tracev( "Counter {0} is already defined on Infinispan data store: Skip creation", counterName );
			}
			return false;
		}

		// global configuration is mandatory in order to define
		// a new clustered counter with persistent storage
		validateGlobalConfiguration();

		// for some executions it is possible that two or more threads will try to define concurrently the same counter:
		// definedByCurrentThread will be true if the counter will be created by current thread
		boolean definedByCurrentThread = counterManager.defineCounter( counterName,
			CounterConfiguration.builder(
				CounterType.UNBOUNDED_STRONG )
					.initialValue( initialValue )
					.storage( Storage.PERSISTENT )
					.build() );

		if ( LOG.isDebugEnabled() && definedByCurrentThread ) {
			LOG.debugv( "Counter {0} defined by current thread", counterName );
		}
		else if ( LOG.isDebugEnabled() ) {
			LOG.debugv( "Counter {0} defined by other concurrent thread", counterName );
		}

		return definedByCurrentThread;

	}

	private void validateGlobalConfiguration() {
		boolean globalConfigIsEnabled = cacheManager.getGlobalComponentRegistry()
			.getGlobalConfiguration().globalState()
			.enabled();

		if ( !globalConfigIsEnabled ) {
			throw LOG.counterCannotBeCreatedWithoutGlobalConfiguration();
		}
	}

	public abstract Number nextValue(NextValueRequest request);

}
