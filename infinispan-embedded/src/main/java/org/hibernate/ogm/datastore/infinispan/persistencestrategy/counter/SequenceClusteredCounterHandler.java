/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.boot.model.relational.Sequence;
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
 * The class is used when the strategy type of ID generator is SequenceGenerator.
 * The required and not declared counters are created by the dialect at start up.
 *
 * @author Fabio Massimo Ercoli
 */
public class SequenceClusteredCounterHandler extends ClusteredCounterHandler {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	// true if the current instance has created the counter
	// and the counter is never used
	private final AtomicBoolean returnTheInitialValue;

	private final StrongCounter counter;

	public SequenceClusteredCounterHandler(EmbeddedCacheManager cacheManager, Sequence sequence) {

		super( cacheManager );

		CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager( cacheManager );

		returnTheInitialValue = new AtomicBoolean(
			defineCounterIfNotExists( sequence.getInitialValue(), counterManager, sequence.getExportIdentifier() )
		);

		if ( LOG.isTraceEnabled() && returnTheInitialValue.get() ) {
			LOG.tracev( "Clustered Counter created for Sequence {0}.", sequence.getExportIdentifier() );
		}

		counter = counterManager.getStrongCounter( sequence.getExportIdentifier() );

	}

	@Override
	public Number nextValue(NextValueRequest request) {

		// return the sequence initial value if it is first use of the counter
		if ( returnTheInitialValue.compareAndSet( true, false ) ) {
			return request.getInitialValue();
		}

		try {
			// consistent "single unit", potentially distributed, increment && get
			return counter.addAndGet( request.getIncrement() ).get();
		}
		catch (ExecutionException | InterruptedException e) {
			LOG.exceptionGeneratingValueForCounter( counter.getName() );
			return null;
		}

	}

}
