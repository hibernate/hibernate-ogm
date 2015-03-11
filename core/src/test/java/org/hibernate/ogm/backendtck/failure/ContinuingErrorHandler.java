/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.failure;

import org.hibernate.ogm.failure.ErrorHandler;
import org.hibernate.ogm.failure.ErrorHandlingStrategy;

/**
 * A test {@link ErrorHandler} which continues after failed operations.
 *
 * @author Gunnar Morling
 */
class ContinuingErrorHandler implements ErrorHandler {

	static ContinuingErrorHandler INSTANCE = new ContinuingErrorHandler();

	@Override
	public void onRollback(RollbackContext context) {
	}

	@Override
	public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
		return ErrorHandlingStrategy.CONTINUE;
	}
}
