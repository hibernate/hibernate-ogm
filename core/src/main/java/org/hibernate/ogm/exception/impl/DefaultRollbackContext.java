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
 * @author Gunnar Morling
 */
public class DefaultRollbackContext implements ErrorHandler.RollbackContext {

	private final List<GridDialectOperation> appliedOperations;

	public DefaultRollbackContext(List<GridDialectOperation> appliedOperations) {
		super();
		this.appliedOperations = new ArrayList<GridDialectOperation>( appliedOperations );
	}

	@Override
	public List<GridDialectOperation> getAppliedOperations() {
		return appliedOperations;
	}
}
