/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.failure.operation.impl;

import org.hibernate.ogm.failure.operation.GridDialectOperation;
import org.hibernate.ogm.failure.operation.OperationType;
import org.hibernate.ogm.failure.operation.ExecuteBatch;

/**
 * @author Gunnar Morling
 *
 */
public class ExecuteBatchImpl implements ExecuteBatch {

	public ExecuteBatchImpl() {
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( ExecuteBatch.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.EXECUTE_BATCH;
	}
}
