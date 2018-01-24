/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.mongodb.errorhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;

/**
 * @author Gunnar Morling
 */
public class TestErrorHandler implements ErrorHandler {

	private static final List<FailedGridDialectOperationContext> onFailedOperationInvocations = Collections.synchronizedList( new ArrayList<FailedGridDialectOperationContext>() );
	private static final List<RollbackContext> onRollbackInvocations = Collections.synchronizedList( new ArrayList<RollbackContext>() );

	@Override
	public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
		onFailedOperationInvocations.add( context );
		return ErrorHandlingStrategy.ABORT;
	}

	@Override
	public void onRollback(RollbackContext context) {
		onRollbackInvocations.add( context );
	}


	public static List<FailedGridDialectOperationContext> getOnFailedOperationInvocations() {
		return onFailedOperationInvocations;
	}

	public static List<RollbackContext> getOnRollbackInvocations() {
		return onRollbackInvocations;
	}
}
