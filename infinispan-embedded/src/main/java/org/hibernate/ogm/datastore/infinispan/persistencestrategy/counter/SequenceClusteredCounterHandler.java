/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan Clustered Counter feature. Used by the dialect to implement a reliable ID generator.
 * The class is used when the strategy type of ID generator is SequenceGenerator. The required and not declared counters
 * are created by the dialect at start up.
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */
public class SequenceClusteredCounterHandler extends ClusteredCounterHandler {

	private final StrongCounter counter;

	public SequenceClusteredCounterHandler(EmbeddedCacheManager cacheManager, Sequence sequence) {
		super( cacheManager );
		counter = getCounterOrCreateIt( sequence.getExportIdentifier(), sequence.getInitialValue() );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return nextValue( request, counter );
	}
}
