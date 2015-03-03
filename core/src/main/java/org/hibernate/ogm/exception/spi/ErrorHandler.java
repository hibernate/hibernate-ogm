/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.spi;

import java.util.List;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;

/**
 * Implementations receive applied/failed operations during specific flush cycles.
 *
 * @author Gunnar Morling
 * @see OgmProperties#ERROR_HANDLER
 */
public interface ErrorHandler {

	void onRollback(RollbackContext context);

	/**
	 * Provides contextual information when notifying an {@link ErrorHandler} about a transaction rollback.
	 */
	interface RollbackContext {

		/**
		 * The operations successfully applied to the datastore before the rollback attempt was made.
		 *
		 * @return An unmodifiable list of successfully applied operations.
		 */
		List<GridDialectOperation> getAppliedOperations();
	}
}
