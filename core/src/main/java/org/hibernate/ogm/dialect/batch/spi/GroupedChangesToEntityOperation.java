/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import java.util.LinkedList;
import java.util.Queue;

import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Wrapper grouping all the update operations for a given entity.
 *
 * @author Guillaume Smet
 */
public class GroupedChangesToEntityOperation implements Operation {

	private final EntityKey entityKey;

	private final Queue<Operation> operations = new LinkedList<>();

	public GroupedChangesToEntityOperation(EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	public void addOperation(Operation operation) {
		operations.add( operation );
	}

	public Queue<Operation> getOperations() {
		return operations;
	}

	public void clear() {
		operations.clear();
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
