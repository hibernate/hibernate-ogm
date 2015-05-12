/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.compensation;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;

/**
 * An {@link ErrorHandler} which makes all its invocations available for testing purposes.
 *
 * @author Gunnar Morling
 */
class InvocationTrackingHandler implements ErrorHandler {

	static InvocationTrackingHandler INSTANCE = new InvocationTrackingHandler();

	private final List<RollbackContext> onRollbackInvocations = new ArrayList<>();
	private final List<FailedGridDialectOperationContext> onFailedOperationInvocations = new ArrayList<>();

	private InvocationTrackingHandler() {
	}

	@Override
	public void onRollback(RollbackContext context) {
		onRollbackInvocations.add( context );
	}

	@Override
	public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
		onFailedOperationInvocations.add( context );
		return ErrorHandlingStrategy.ABORT;
	}

	public void clear() {
		onRollbackInvocations.clear();
		onFailedOperationInvocations.clear();
	}

	public List<RollbackContext> getOnRollbackInvocations() {
		return onRollbackInvocations;
	}

	public List<FailedGridDialectOperationContext> getOnFailedOperationInvocations() {
		return onFailedOperationInvocations;
	}
}
