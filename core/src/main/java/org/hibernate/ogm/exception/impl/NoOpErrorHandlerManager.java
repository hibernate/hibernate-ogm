/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.List;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;

/**
 * A {@link ErrorHandlerManager} which discards all operations.
 *
 * @author Gunnar Morling
 */
public class NoOpErrorHandlerManager implements ErrorHandlerManager {

	public static final NoOpErrorHandlerManager INSTANCE = new NoOpErrorHandlerManager();

	private NoOpErrorHandlerManager() {
	}

	@Override
	public void afterFlush(List<GridDialectOperation> appliedOperations) {
	}

	@Override
	public void onRollback() {
	}
}
