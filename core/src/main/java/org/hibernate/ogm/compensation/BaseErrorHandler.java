/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation;

/**
 * Default (no-op) implementation of {@link ErrorHandler}.
 *
 * @author Gunnar Morling
 */
public class BaseErrorHandler implements ErrorHandler {

	@Override
	public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
		// no-op

		return ErrorHandlingStrategy.ABORT;
	}

	@Override
	public void onRollback(RollbackContext context) {
		// no-op
	}
}
