/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter;

import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan Clustered Counter feature. Used by the dialect to implement a reliable ID generator.
 * The class is used when the strategy type of ID generator is TableGenerator. The required and not declared counters
 * are created by the dialect at runtime at first use.
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */
public class TableClusteredCounterHandler extends ClusteredCounterHandler {

	public TableClusteredCounterHandler(EmbeddedCacheManager cacheManager) {
		super( cacheManager );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		String counterName = counterName( request );
		StrongCounter strongCounter = getCounterOrCreateIt( counterName, request.getInitialValue() );
		return nextValue( request, strongCounter );
	}

	private String counterName(NextValueRequest request) {
		StringBuilder builder = new StringBuilder();
		builder.append( request.getKey().getTable() );
		builder.append( "." );
		builder.append( request.getKey().getColumnValue() );
		return builder.toString();
	}
}
