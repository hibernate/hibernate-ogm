/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.transaction.emulated.impl.EmulatedLocalTransactionCoordinatorBuilder;
import org.hibernate.ogm.transaction.jta.impl.RollbackOnCommitFailureJtaTransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;

/**
 * Recommended base class for {@link DatastoreProvider} implementations.
 *
 * @author Gunnar Morling
 *
 */
public abstract class BaseDatastoreProvider implements DatastoreProvider {

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return null;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return BaseSchemaDefiner.class;
	}

	/**
	 * Whether this underlying datastore allows emulation of transactions.
	 *
	 * When transaction emulation is used, transactions only demarcate a unit of work. The transaction emulation will
	 * make sure that at commit time all required changes are flushed, but there are otherwise no true transaction,
	 * in particular rollback, semantics.
	 *
	 * @return {@code true} if the underlying datastore allows transaction emulation, {@code false} otherwise.
	 *
	 */
	protected boolean allowsTransactionEmulation() {
		return false;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder, StrategySelector strategySelector) {
		if ( coordinatorBuilder.isJta() ) {
			return coordinatorBuilder;
		}
		else if ( allowsTransactionEmulation() ) {
			// if the datastore does not support transactions it is enough to emulate them. In this case transactions
			// are just used to scope a unit of work and to make sure that the appropriate flush event occurs
			return new EmulatedLocalTransactionCoordinatorBuilder( jdbcDefaultBuilder( strategySelector ) );
		}
		else {
			return new RollbackOnCommitFailureJtaTransactionCoordinatorBuilder( jtaDefaultBuilder( strategySelector ) );
		}
	}

	protected TransactionCoordinatorBuilder jdbcDefaultBuilder(StrategySelector strategySelector) {
		return strategySelector.resolveStrategy( TransactionCoordinatorBuilder.class, "jdbc" );
	}

	protected TransactionCoordinatorBuilder jtaDefaultBuilder(StrategySelector strategySelector) {
		return strategySelector.resolveStrategy( TransactionCoordinatorBuilder.class, "jta" );
	}
}
