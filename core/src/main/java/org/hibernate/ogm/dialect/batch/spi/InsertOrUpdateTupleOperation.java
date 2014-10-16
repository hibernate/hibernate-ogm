/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Contains the data required to update a tuple
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class InsertOrUpdateTupleOperation implements Operation {

	private final Tuple tuple;
	private final EntityKey entityKey;
	private final TupleContext tupleContext;

	public InsertOrUpdateTupleOperation(Tuple tuple, EntityKey entityKey, TupleContext tupleContext) {
		this.tuple = tuple;
		this.entityKey = entityKey;
		this.tupleContext = tupleContext;
	}

	public Tuple getTuple() {
		return tuple;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	public TupleContext getTupleContext() {
		return tupleContext;
	}
}
