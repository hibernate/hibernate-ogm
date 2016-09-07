/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Contains the data required to update a tuple
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class InsertOrUpdateTupleOperation implements GroupableEntityOperation {

	private final TuplePointer tuplePointer;
	private final EntityKey entityKey;
	private final TupleContext tupleContext;

	public InsertOrUpdateTupleOperation(TuplePointer tuplePointer, EntityKey entityKey, TupleContext tupleContext) {
		this.tuplePointer = tuplePointer;
		this.entityKey = entityKey;
		this.tupleContext = tupleContext;
	}

	public TuplePointer getTuplePointer() {
		return tuplePointer;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	public TupleContext getTupleContext() {
		return tupleContext;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "[" );
		sb.append( entityKey );
		sb.append( "]" );
		return sb.toString();
	}

}
