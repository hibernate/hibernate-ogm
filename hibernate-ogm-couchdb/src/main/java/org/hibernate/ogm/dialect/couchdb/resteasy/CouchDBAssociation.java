/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.resteasy;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBAssociationSnapshot;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBTuple;
import org.hibernate.ogm.dialect.couchdb.model.CouchDBTupleSnapshot;
import org.hibernate.ogm.dialect.couchdb.util.Identifier;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Contains the information related to an {@link Association}
 *
 * Used to serialize and deserialize the JSON with the following structure:
 *
 * { "_id": "", "_rev": " ", "type": "CouchDBAssociation", "tuples": [{ "tupleColumnValues": [] }], "tupleColumnNames":
 * [] }
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public class CouchDBAssociation extends CouchDBDocument {

	private Set<String> tupleColumnNames;
	private Set<AssociationTuple> tuples = new HashSet<AssociationTuple>();

	private Map<String, Integer> columnNamesPositions;

	private Identifier identifier = new Identifier();

	public CouchDBAssociation() {
	}

	public CouchDBAssociation(AssociationKey associationKey) {
		setId( createId( associationKey ) );
	}

	/**
	 * Update the CouchDBAssociation with the data from {@link Association}
	 *
	 * @param association
	 */
	@JsonIgnore
	public void update(Association association) {
		updateColumnNames( association );
		updateTuples( association );
	}

	/**
	 * Return the {@link Tuple} with the {@link RowKey}
	 *
	 * @param key
	 *            of the searched Tuple
	 * @return the found tuple
	 */
	@JsonIgnore
	public Tuple getTuple(RowKey key) {
		return new Tuple( new CouchDBTupleSnapshot( buildTuple( key ) ) );
	}

	/**
	 * Reconstruct the Association using the supplied {@link AssociationKey} and the CouchDBAssociation
	 * The AssociationKey is needed because the information about the Keys are not stored.
	 *
	 * @param key
	 *            used to reconstruct the Association
	 * @return the Association from the CouchDBAssociation
	 */
	@JsonIgnore
	public Association getAssociation(AssociationKey key) {
		return new Association( new CouchDBAssociationSnapshot( this, key ) );
	}

	/**
	 * Check if the CouchDBAssociation contains a Tuple with the supplied {@link RowKey}
	 *
	 * @param key
	 * @return true if contains the RowKey, false otherwise
	 */
	public boolean containsKey(RowKey key) {
		for ( AssociationTuple tuple : tuples ) {
			if ( tuple.hasKey( columnNamesPositions, key ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all the key of the CouchDBAssociation
	 * The AssociationKey is needed because the information about the Keys are not stored.
	 *
	 * @param associationKey
	 *            used to reconstruct the RowKeys
	 * @return
	 */
	@JsonIgnore
	public Set<RowKey> getRowKeys(AssociationKey associationKey) {
		Set<RowKey> result = new HashSet<RowKey>();
		final String[] rowKeycolumnNames = associationKey.getRowKeyColumnNames();
		final String tableName = associationKey.getTable();

		for ( AssociationTuple tuple : tuples ) {
			result.add( tuple.getRowKey( rowKeycolumnNames, tableName, columnNamesPositions ) );
		}
		return result;
	}

	/**
	 * Return the number of Tuples of the CouchDBAssociation
	 *
	 * @return the number of tuples
	 */
	public int size() {
		return tuples.size();
	}

	/**
	 * getter and setter used for serialization/deserialization purposes
	 *
	 */

	public Set<String> getTupleColumnNames() {
		return tupleColumnNames;
	}

	public void setTupleColumnNames(Set<String> tupleColumnNames) {
		this.tupleColumnNames = tupleColumnNames;
		columnNamesPositions = getColumnNamesPositions( tupleColumnNames );
	}

	public Set<AssociationTuple> getTuples() {
		return tuples;
	}

	public void setTuples(Set<AssociationTuple> tuples) {
		this.tuples = tuples;
	}

	private CouchDBTuple buildTuple(RowKey key) {
		return new CouchDBTuple( tupleColumnNames, getTupleColumnValues( key ) );
	}

	private String createId(AssociationKey associationKey) {
		return identifier.createAssociationId( associationKey );
	}

	private void updateTuples(Association association) {
		tuples.clear();
		final Set<RowKey> keys = association.getKeys();
		for ( RowKey key : keys ) {
			addTuple( key, new CouchDBTuple( association.get( key ) ) );
		}
	}

	private void updateColumnNames(Association association) {
		tupleColumnNames = getTupleColumnNames( association );
		columnNamesPositions = getColumnNamesPositions( tupleColumnNames );
	}

	private Set<String> getTupleColumnNames(Association association) {
		return getATuple( association ).getColumnNames();
	}

	private Map<String, Integer> getColumnNamesPositions(Set<String> tupleColumnNames) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		int position = 0;
		for ( String columnName : tupleColumnNames ) {
			result.put( columnName, position );
			position++;
		}
		return result;
	}

	private Tuple getATuple(Association association) {
		final Iterator<RowKey> iterator = association.getKeys().iterator();
		return association.get( iterator.next() );
	}

	private Object[] getTupleColumnValues(RowKey key) {
		for ( AssociationTuple tuple : tuples ) {
			if ( tuple.hasKey( columnNamesPositions, key ) ) {
				return tuple.getTupleColumnValues();
			}
		}
		return null;
	}

	private void addTuple(RowKey key, CouchDBTuple tuple) {
		tuples.add( new AssociationTuple( tuple.getColumnValues() ) );
	}

}
