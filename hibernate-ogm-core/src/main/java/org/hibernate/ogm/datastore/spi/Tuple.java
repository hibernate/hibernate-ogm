/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.spi;

import static org.hibernate.ogm.datastore.spi.TupleOperationType.PUT;
import static org.hibernate.ogm.datastore.spi.TupleOperationType.PUT_NULL;
import static org.hibernate.ogm.datastore.spi.TupleOperationType.REMOVE;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.impl.SetFromCollection;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Represents a Tuple (think of it as a row)
 * 
 * A tuple accepts a TupleShapshot which is a read-only state of the tuple at
 * creation time.
 * 
 * A tuple collects changes applied to it. These changes are represented by a
 * list of TupleOperation. It is intended that GridDialects retrieve to these
 * actions and reproduce them to the datastore. The list of changes is computed
 * based off the snapshot.
 * 
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class Tuple {

	private static final Log log = LoggerFactory.make();
	private final TupleSnapshot snapshot;
	private Map<String, TupleOperation> currentState = null; // lazy initialize
																// the Map as it
																// costs quite
																// some memory
	private final JSONedClassDetector jsonedDetector = new JSONedClassDetector();
	private final JSONHelper jsonHelper = new JSONHelper(
			new WrapperClassDetector(), jsonedDetector);

	public Tuple(TupleSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public Object get(String column) {
		if (currentState == null) {
			return snapshot.get(column);
		}
		TupleOperation result = currentState.get(column);
		if (result == null) {
			return snapshot.get(column);
		} else if (result.getType() == PUT_NULL || result.getType() == REMOVE) {
			return null;
		} else {
			return result.getValue();
		}
	}

	public void put(String column, Object value) {
		if (currentState == null) {
			currentState = new HashMap<String, TupleOperation>();
		}
		if (value == null) {
			currentState
					.put(column, new TupleOperation(column, null, PUT_NULL));
		} else {
			currentState.put(column, new TupleOperation(column, value, PUT));
		}
	}

	public void remove(String column) {
		if (currentState == null) {
			currentState = new HashMap<String, TupleOperation>();
		}
		currentState.put(column, new TupleOperation(column, null, REMOVE));
	}

	/**
	 * Return the list of actions on the tuple. Inherently deduplicated
	 * operations
	 */
	public Set<TupleOperation> getOperations() {
		if (currentState == null) {
			return Collections.emptySet();
		} else {
			return new SetFromCollection<TupleOperation>(currentState.values());
		}
	}

	public TupleSnapshot getSnapshot() {
		return snapshot;
	}

	public Set<String> getColumnNames() {
		if (currentState == null) {
			return snapshot.getColumnNames();
		}
		Set<String> columnNames = new HashSet<String>(snapshot.getColumnNames());
		for (TupleOperation op : currentState.values()) {
			switch (op.getType()) {
			case PUT:
			case PUT_NULL:
				columnNames.add(op.getColumn());
				break;
			case REMOVE:
				columnNames.remove(op.getColumn());
				break;
			}
		}
		return columnNames;
	}

	/**
	 * Gets column values.
	 * 
	 * @return Set<Object> All the corresponding column values. If there are no
	 *         column names, then returns an empty Set.
	 */
	public Set<Object> getColumnValues() {

		Set<String> columnNames = getColumnNames();
		if (currentState == null || columnNames.isEmpty() == true) {
			return Collections.EMPTY_SET;
		}

		Set<Object> columnValues = new HashSet<Object>();
		for (String columnName : columnNames) {
			columnValues.add(currentState.get(columnName));
		}

		return Collections.unmodifiableSet(columnValues);
	}

	/**
	 * Gets snapshot as Map.
	 * 
	 * @return Map<String,Object> All the column name and value pairs as Map.
	 */
	public Map<String, Object> getSnapShotAsMap() {
		Set<String> columnNames = getColumnNames();
		if (currentState == null || columnNames.isEmpty() == true) {
			return Collections.EMPTY_MAP;
		}

		return this.putJSONedValueAsNeeded(columnNames);
	}

	/**
	 * Changes the value for the column as JSON format.
	 * 
	 * @param columnNames
	 *            All the columnNames in the entity object.
	 * @return Newly created Map storing JSON format when required.
	 * @throws ClassNotFoundException
	 */
	private Map<String, Object> putJSONedValueAsNeeded(Set<String> columnNames) {

		Map<String, Object> map = new HashMap<String, Object>();

		for (String columnName : columnNames) {
			if (snapshot.get(columnName) == null) {
				map.put(columnName, null);
			} else if (snapshot.get(columnName).getClass().isArray()) {
				map.put(columnName, jsonHelper.toJSON(snapshot.get(columnName)));
			} else if (this.jsonedDetector.isAssignable(snapshot
					.get(columnName).getClass())) {
				map.put(columnName, jsonHelper.toJSON(snapshot.get(columnName)));
			} else {
				map.put(columnName, snapshot.get(columnName));
			}
		}
		return map;
	}
}
