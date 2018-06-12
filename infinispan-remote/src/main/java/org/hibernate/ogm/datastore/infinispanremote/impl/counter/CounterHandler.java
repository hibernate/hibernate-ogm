/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.counter;

import java.util.concurrent.ExecutionException;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.dialect.spi.NextValueRequest;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.StrongCounter;

/**
 * Handle a single {@link StrongCounter} to implement id source
 * defined with {@link javax.persistence.SequenceGenerator}.
 *
 * Constructor is not thread safe. Initialization phase is performed by a single thread.
 * {@link CounterHandler#nextValue(NextValueRequest)} is thread safe,
 * relying on safety on {@link StrongCounter#incrementAndGet()}.
 *
 * @author Fabio Massimo Ercoli
 */
public class CounterHandler {

	private final String counterName;
	private final StrongCounter counter;

	public CounterHandler(RemoteCacheManager cacheManager, Sequence sequence) {
		CounterManager counterManager = RemoteCounterManagerFactory.asCounterManager( cacheManager );
		counterName = sequence.getExportIdentifier();

		if ( !counterManager.isDefined( counterName ) ) {
			defineCounter( counterManager, counterName, sequence.getInitialValue() );
		}
		counter = counterManager.getStrongCounter( counterName );
	}

	public Number nextValue(NextValueRequest request) {
		try {
			Long newValue = counter.addAndGet( request.getIncrement() ).get();
			return newValue - request.getIncrement();
		}
		catch (ExecutionException | InterruptedException e) {
			throw new HibernateException( "Interrupting Operation " + e.getMessage(), e );
		}
	}

	public String getCounterName() {
		return counterName;
	}

	private void defineCounter(CounterManager counterManager, String counterName, int initialValue) {
		counterManager.defineCounter(
				counterName,
				CounterConfiguration.builder(
						CounterType.UNBOUNDED_STRONG )
						.initialValue( initialValue )
						.storage( Storage.PERSISTENT )
						.build()
		);
	}
}
