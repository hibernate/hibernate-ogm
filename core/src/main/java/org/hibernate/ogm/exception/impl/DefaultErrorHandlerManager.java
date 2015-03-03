/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.spi.ErrorHandler;

/**
 * {@link ErrorHandlerManager} which triggers to the {@link ErrorHandler} configured by the user.
 *
 * @author Gunnar Morling
 */
public class DefaultErrorHandlerManager implements ErrorHandlerManager {

	private final ErrorHandler errorHandler;
	private final List<GridDialectOperation> appliedOperations;

	public DefaultErrorHandlerManager(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		this.appliedOperations = new ArrayList<>();
	}

	@Override
	public void afterFlush(List<GridDialectOperation> appliedOperations) {
		this.appliedOperations.addAll( appliedOperations );
	}

	@Override
	public void onRollback() {
		DefaultRollbackContext context = new DefaultRollbackContext( appliedOperations );
		errorHandler.onRollback( context );
	}
}
