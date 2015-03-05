/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.ExecuteBatch;
import org.hibernate.ogm.exception.operation.spi.OperationType;

/**
 * @author Gunnar Morling
 *
 */
public class ExecuteBatchImpl extends AbstractGridDialectOperation implements ExecuteBatch {

	public ExecuteBatchImpl() {
		super( ExecuteBatch.class, OperationType.EXECUTE_BATCH );
	}
}
