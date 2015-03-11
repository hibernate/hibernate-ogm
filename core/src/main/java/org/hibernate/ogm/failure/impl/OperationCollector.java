/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.failure.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.failure.ErrorHandler;
import org.hibernate.ogm.failure.operation.GridDialectOperation;

/**
 * Collects the grid dialect operations applied in the course of one transaction.
 *
 * @author Gunnar Morling
 */
public class OperationCollector implements ErrorHandler.RollbackContext {

	private final List<GridDialectOperation> appliedOperations;

	public OperationCollector() {
		this.appliedOperations = new ArrayList<>();
	}

	public void addAppliedOperation(GridDialectOperation operation) {
		appliedOperations.add( operation );
	}

	@Override
	public List<GridDialectOperation> getAppliedGridDialectOperations() {
		return Collections.unmodifiableList( appliedOperations );
	}
}
