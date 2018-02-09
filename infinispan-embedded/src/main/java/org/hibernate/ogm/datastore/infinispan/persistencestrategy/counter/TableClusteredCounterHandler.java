/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutionException;

import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.spi.NextValueRequest;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan Clustered Counter feature.
 * Used by the dialect to implement a reliable ID generator.
 *
 * The class is used when the strategy type of ID generator is TableGenerator.
 * The required and not declared counters are created by the dialect at runtime at first use.
 *
 * @author Fabio Massimo Ercoli
 */
public class TableClusteredCounterHandler extends ClusteredCounterHandler {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	public TableClusteredCounterHandler(EmbeddedCacheManager cacheManager) {
		super( cacheManager );
	}

	public Number nextValue(NextValueRequest request) {

		CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager( cacheManager );
		String counterName = request.getKey().getColumnValue();

		// try to define new counter at runtime
		boolean definedByCurrentThread = defineCounterIfNotExists( request.getInitialValue(), counterManager, counterName );
		if ( definedByCurrentThread ) {

			LOG.tracev( "Clustered Counter created for Sequence %1$s.", counterName );
			return Long.valueOf( request.getInitialValue() );
		}

		StrongCounter strongCounter = counterManager.getStrongCounter( counterName );
		try {
			// consistent "single unit", potentially distributed, increment && get
			return strongCounter.addAndGet( request.getIncrement() ).get();
		}
		catch (ExecutionException | InterruptedException e) {
			LOG.exceptionGeneratingValueForCounter( counterName );
			return null;
		}

	}

}
