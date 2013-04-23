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
import org.hibernate.ogm.dialect.couchdb.model.CouchDBTuple;
import org.hibernate.ogm.dialect.couchdb.util.Identifier;
import org.hibernate.ogm.grid.EntityKey;

/**
 * Contains the information related to a {@link org.hibernate.ogm.datastore.spi.Tuple}
 *
 * The use of this class is to serialize and deserialize the JSON stored in CouchDB has the following structure:
 * { "_id": "", "_rev": "", "type": "CouchDBEntity", "columnNames":
 * [],"columnValues": [ ],
 * "tableName": "user" }
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public class CouchDBEntity extends CouchDBDocument {

	private Identifier identifier = new Identifier();

	private String tableName;
	private String[] columnNames;
	private Object[] columnValues;

	CouchDBEntity() {
	}

	public CouchDBEntity(EntityKey key) {
		setId( identifier.createEntityId( key ) );
		tableName = key.getTable();
		columnNames = key.getColumnNames();
		columnValues = key.getColumnValues();
	}

	public void update(CouchDBTuple tuple) {
		columnNames = tuple.getColumnNames();
		columnValues = tuple.getColumnValues();
	}

	@JsonIgnore
	public CouchDBTuple getTuple() {
		return new CouchDBTuple( columnNames, columnValues );
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	public void setColumnValues(Object[] columnValues) {
		this.columnValues = columnValues;
	}

}
