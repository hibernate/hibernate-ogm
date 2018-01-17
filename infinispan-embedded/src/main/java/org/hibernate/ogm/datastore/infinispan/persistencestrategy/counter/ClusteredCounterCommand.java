/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import java.util.concurrent.ExecutionException;

import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author Fabio Massimo Ercoli (C) 2017 Red Hat Inc.
 */
public class ClusteredCounterCommand {

	public Number nextValue(EmbeddedCacheManager cacheManager, NextValueRequest request) {

		CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager( cacheManager );

		// for @SequenceGenerator counter name will be the sequenceName
		// for @TableGenerator counter name will be the columnValue
		String counterName =
			( IdSourceKeyMetadata.IdSourceType.SEQUENCE.equals( request.getKey().getMetadata().getType() ) ) ?
				request.getKey().getTable() :
				request.getKey().getColumnValue();

		if ( !counterManager.isDefined( counterName ) ) {

			// try to define new counter at runtime
			boolean definedByCurrentThread = counterManager.defineCounter( counterName,
				CounterConfiguration.builder( CounterType.UNBOUNDED_STRONG )
					.initialValue( request.getInitialValue() )
					.storage( Storage.VOLATILE )
					.build() );

			if ( definedByCurrentThread ) {
				return Long.valueOf( request.getInitialValue() );
			}

		}

		StrongCounter strongCounter = counterManager.getStrongCounter( counterName );
		try {
			// consistent "single unit" increment && get
			return strongCounter.addAndGet( request.getIncrement() ).get();
		}
		catch (ExecutionException | InterruptedException e) {
			throw new HibernateException( "Interrupting Operation " + e.getMessage(), e );
		}

	}

}
