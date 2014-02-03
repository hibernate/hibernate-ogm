/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.batch;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A queue for {@link Operation}.
 * <p>
 * It keeps track of the element that are going to be affected by an {@link UpdateTupleOperation}
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Davide D'Alto <davide@hibernate.com>
 */
public class OperationsQueue {

	private static final Log log = LoggerFactory.make();

	private final Queue<Operation> operations = new LinkedList<Operation>();

	private final Set<EntityKey> entityKeys = new HashSet<EntityKey>();

	public void add(UpdateTupleOperation operation) {
		entityKeys.add( operation.getEntityKey() );
		addOperation( operation );
	}

	public void add(Operation operation) {
		addOperation( operation );
	}

	private void addOperation(Operation operation) {
		log.debug( "Add batched operation " + operation );
		operations.add( operation );
	}

	public Operation poll() {
		Operation operation = operations.poll();
		entityKeys.remove( operation );
		return operation;
	}

	public void clear() {
		entityKeys.clear();
		operations.clear();
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

}
