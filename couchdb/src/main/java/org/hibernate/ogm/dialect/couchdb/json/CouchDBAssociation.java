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
package org.hibernate.ogm.dialect.couchdb.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * Contains the information related to an {@link Association}
 *
 * Used to serialize and deserialize the JSON with the following structure:
 *
 * { "_id": "", "_rev": " ", "$type": "CouchDBAssociation", "rows": [{ "key": "value" }] }
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeName(CouchDBAssociation.TYPE_NAME)
public class CouchDBAssociation extends CouchDBDocument {

	/**
	 * The name of this document type as materialized in {@link CouchDBDocument#TYPE_DISCRIMINATOR_FIELD_NAME}.
	 */
	public static final String TYPE_NAME = "association";

	private List<Map<String, Object>> rows = new ArrayList<Map<String,Object>>();

	public CouchDBAssociation() {
	}

	public CouchDBAssociation(String id) {
		super( id );
	}

	/**
	 * Updates the CouchDBAssociation with the data from {@link Association}
	 *
	 * @param association
	 *            used to update the CouchDBAssociation
	 */
	@JsonIgnore
	public void update(Association association, AssociationKey associationKey) {
		rows.clear();

		for ( RowKey rowKey : association.getKeys() ) {
			Tuple tuple = association.get( rowKey );

			Map<String, Object> row = new HashMap<String, Object>();
			for ( String columnName : tuple.getColumnNames() ) {
				// don't store columns which are part of the assocation key and can be retrieved from there
				if ( !isKeyColumn( associationKey, columnName ) ) {
					row.put( columnName, tuple.get( columnName ) );
				}
			}

			rows.add( row );
		}
	}

	private boolean isKeyColumn(AssociationKey associationKey, String columnName) {
		for ( String keyColumName : associationKey.getColumnNames() ) {
			if ( keyColumName.equals( columnName ) ) {
				return true;
			}
		}

		return false;
	}

	public List<Map<String,Object>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
}
