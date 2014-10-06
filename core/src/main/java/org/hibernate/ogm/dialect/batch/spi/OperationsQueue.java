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

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A queue for {@link Operation}.
 * <p>
 * It keeps track of the element that are going to be affected by an {@link UpdateTupleOperation}.
 * The queue can be closed, in that case it will throw an exception when trying to add or poll an operation.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OperationsQueue {

	/**
	 * A queue that it is always closed
	 */
	public static final OperationsQueue CLOSED_QUEUE = new OperationsQueue() {
		@Override
		public boolean isClosed() {
			return true;
		}
	};

	private static final Log log = LoggerFactory.make();

	private final Queue<Operation> operations = new LinkedList<Operation>();

	private final Set<EntityKey> entityKeys = new HashSet<EntityKey>();

	private final Map<EntityKey, Map<AssociationKey, Association>> associations = new HashMap<EntityKey, Map<AssociationKey, Association>>();

	private boolean closed = false;

	public void add(UpdateTupleOperation operation) {
		validate();
		entityKeys.add( operation.getEntityKey() );
		addOperation( operation );
	}

	public void add(Operation operation) {
		validate();
		addOperation( operation );
	}

	private void validate() {
		if ( isClosed() ) {
			throw log.closedOperationQueue();
		}
	}

	private void addOperation(Operation operation) {
		log.debugf( "Add batched operation %1$s", operation );
		operations.add( operation );
	}

	public Operation poll() {
		validate();
		Operation operation = operations.poll();
		if (operation instanceof UpdateTupleOperation ) {
			UpdateTupleOperation update = (UpdateTupleOperation) operation;
			entityKeys.remove( update.getEntityKey() );
			associations.remove( update.getEntityKey() );
		}
		return operation;
	}

	public void close() {
		entityKeys.clear();
		operations.clear();
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param key the {@link EntityKey} that identify the element
	 * @return true if an {@link UpdateTupleOperation} is bound to the key, false otherwise
	 */
	public boolean contains(EntityKey key) {
		return entityKeys.contains( key );
	}

	/**
	 * @return the length of the queue
	 */
	public int size() {
		return operations.size();
	}

	/**
	 * Check if the queue contains an association for the given {@link EntityKey} and {@link AssociationKey}
	 *
	 * @param entityKey the key of the entity storing the association
	 * @param associationKey the key identifying the association
	 * @return true if there is an association in the queue, false otherwise
	 */
	public boolean containsAssociation(EntityKey entityKey, AssociationKey associationKey) {
		return ( associations.containsKey( entityKey ) && associations.get( entityKey ).containsKey( associationKey ) );
	}

	/**
	 * Return the {@link Association} in the queue corresponding to the given {@link EntityKey} and {@link AssociationKey}.
	 * <p>
	 * The association won't be removed from the queue
	 *
	 * @param entityKey the key of the entity storing the association
	 * @param associationKey the key identifying the association
	 * @return the queued {@link Association}
	 */
	public Association getAssociation(EntityKey entityKey, AssociationKey associationKey) {
		return associations.get( entityKey ).get( associationKey );
	}

	/**
	 * Put an {@link Association} in the queue for the given {@link EntityKey} and {@link AssociationKey}.
	 * <p>
	 * If an {@link Association} already exists for the given parameters, the new association will override the old one.
	 *
	 * @param entityKey the key of the entity storing the association
	 * @param associationKey the key identifying the association
	 * @param association the {@link Association} to add to the queue
	 */
	public void putAssociation(EntityKey entityKey, AssociationKey associationKey, Association association) {
		if ( !associations.containsKey( entityKey ) ) {
			associations.put( entityKey, new HashMap<AssociationKey, Association>() );
		}
		associations.get( entityKey ).put( associationKey, association );
	}
}
