/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.Service;

/**
 * Provides datastore-centric configurations and native access.
 * <p>
 * Implementations of this service offer native interfaces to access the underlying datastore. They are also responsible
 * for starting and stopping the connection to the datastore.
 * <p>
 * Instead of implementing this interface directly, consider to extend {@link BaseDatastoreProvider} instead.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public interface DatastoreProvider extends Service {

	/**
	 * Returns the {@link GridDialect} type for the underlying datastore.
	 *
	 * @return The {@link GridDialect} type; Never {@code null}.
	 */
	Class<? extends GridDialect> getDefaultDialect();

	/**
	 * Returns the type of {@link QueryParserService} to be used for executing queries against the underlying datastore.
	 *
	 * @return The query parser implementation type of the current dialect or {@code null} if the underlying datastore
	 * does not support the execution of queries and full-text searches via Lucene / Hibernate Search are to be used
	 * instead.
	 */
	Class<? extends QueryParserService> getDefaultQueryParserServiceType();

	/**
	 * Returns the type of the {@link SchemaDefiner} of this datastore. An instance of this type will be added to the
	 * service registry using the {@link SchemaDefiner} service role.
	 *
	 * @return the schema definer type
	 */
	Class<? extends SchemaDefiner> getSchemaDefinerType();

	/**
	 * Whether the underlying datastore allows emulation of transactions.
	 * <p>
	 * Only of relevance if this provider does not return a custom {@link TransactionCoordinatorBuilder} from
	 * {@link #getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder)}.
	 * <p>
	 * When transaction emulation is used, transactions only demarcate a unit of work. The transaction emulation will
	 * make sure that at commit time all required changes are flushed, but there are otherwise no true transaction, in
	 * particular rollback, semantics.
	 *
	 * @return {@code true} if the underlying datastore allows transaction emulation, {@code false} otherwise.
	 */
	boolean allowsTransactionEmulation();

	/**
	 * Allow the {@link DatastoreProvider} to replace/wrap the current {@link TransactionCoordinatorBuilder}.
	 *
	 * @param coordinatorBuilder the current {@link TransactionCoordinatorBuilder}
	 * @return the same {@link TransactionCoordinatorBuilder}, or a new one if database needs additional functionalities.
	 */
	TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder);
}
