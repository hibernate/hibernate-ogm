/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.operation.spi.OperationType;

/**
 *
 * @author Gunnar Morling
 *
 */
abstract class AbstractGridDialectOperation implements GridDialectOperation {

	private final Class<? extends GridDialectOperation> type;
	private final OperationType operationType;

	AbstractGridDialectOperation(Class<? extends GridDialectOperation> type, OperationType operationType) {
		this.type = type;
		this.operationType = operationType;
	}

	@Override
	public OperationType getType() {
		return operationType;
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( this.type.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}
}
