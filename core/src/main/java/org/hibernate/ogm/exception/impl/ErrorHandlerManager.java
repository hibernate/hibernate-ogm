/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.List;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.spi.ErrorHandler;

/**
 * Manages the {@link ErrorHandler}, if present.
 *
 * @author Gunnar Morling
 */
public interface ErrorHandlerManager {

	/**
	 * Captures the applied operations of the current flush cycle from the collector before they are cleared.
	 */
	void afterFlush(List<GridDialectOperation> appliedOperations);

	void onRollback();
}
