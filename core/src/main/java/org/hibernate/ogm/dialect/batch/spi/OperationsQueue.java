/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * A queue for {@link Operation}s.
 * <p>
 * It keeps track of the elements that are going to be affected by an {@link InsertOrUpdateTupleOperation}.
 * The queue can be closed, in that case it will throw an exception when trying to add or poll an operation.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Smet
 */
public class OperationsQueue {

	/**
	 * A queue that is always closed
	 */
	public static final OperationsQueue CLOSED_QUEUE = new OperationsQueue() {
		@Override
		public boolean isClosed() {
			return true;
		}
	};

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Queue<Operation> operations = new LinkedList<Operation>();

	private final Map<EntityKey, GroupedChangesToEntityOperation> groupedOperations = new HashMap<>();

	private final Set<EntityKey> insertionQueue = new HashSet<>();

	private boolean closed = false;

	public OperationsQueue() {
	}

	public void add(Operation operation) {
		validate();

		log.debugf( "Add batched operation %1$s", operation );

		if ( operation instanceof InsertOrUpdateTupleOperation ) {
			addInsertOrUpdateTupleOperation( (InsertOrUpdateTupleOperation) operation );
		}
		else if ( operation instanceof GroupableEntityOperation ) {
			addGroupableEntityOperation( (GroupableEntityOperation) operation );
		}
		else {
			addOperation( operation );
		}
	}

	private void addInsertOrUpdateTupleOperation(InsertOrUpdateTupleOperation operation) {
		addGroupableEntityOperation( operation );
		insertionQueue.add( operation.getEntityKey() );
	}

	private void addGroupableEntityOperation(GroupableEntityOperation operation) {
		validate();
		GroupedChangesToEntityOperation groupedOperation = getOrCreateGroupedChangesOnEntityOperation( operation.getEntityKey() );
		groupedOperation.addOperation( operation );
	}

	private void addOperation(Operation operation) {
		operations.add( operation );
	}

	private GroupedChangesToEntityOperation getOrCreateGroupedChangesOnEntityOperation(EntityKey entityKey) {
		GroupedChangesToEntityOperation groupedOperation = groupedOperations.get( entityKey );
		if ( groupedOperation == null ) {
			groupedOperation = new GroupedChangesToEntityOperation( entityKey );
			groupedOperations.put( entityKey, groupedOperation );
			addOperation( groupedOperation );
		}
		return groupedOperations.get( entityKey );
	}

	private void validate() {
		if ( isClosed() ) {
			throw log.closedOperationQueue();
		}
	}

	public Operation poll() {
		validate();
		return operations.poll();
	}

	public void clear() {
		groupedOperations.clear();
		operations.clear();
		insertionQueue.clear();
	}

	public void close() {
		clear();
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param key the {@link EntityKey} that identify the element
	 * @return true if an {@link InsertOrUpdateTupleOperation} is bound to the key, false otherwise
	 */
	public boolean isInTheInsertionQueue(EntityKey key) {
		return insertionQueue.contains( key );
	}

	/**
	 * @return the length of the queue
	 */
	public int size() {
		return operations.size();
	}

}
