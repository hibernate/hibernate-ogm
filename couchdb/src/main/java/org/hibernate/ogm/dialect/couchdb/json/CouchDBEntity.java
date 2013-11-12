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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.couchdb.util.Identifier;
import org.hibernate.ogm.grid.EntityKey;

/**
 * Contains the information related to a {@link org.hibernate.ogm.datastore.spi.Tuple} The use of this class is to
 * serialize and deserialize the JSON stored in CouchDB; Documents have the following structure:
 *
 * <pre>
 * {@code
 * {
 *     "_id": "a4jdefe8",
 *     "_rev": "123",
 *     "type": "CouchDBEntity",
 *     "tableName": "Foo",
 *
 *     "name": "Bob",
 *     "login": "dude",
 *     "homeAddress": {
 *         "street2": null,
 *         "street1": "1 avenue des Champs Elysees",
 *         "country": "France",
 *         "city": "Paris"
 *     },
 * }
 * }
 * </pre>
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public class CouchDBEntity extends CouchDBDocument {

	private static final String PATH_SEPARATOR = ".";
	private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile( Pattern.quote( PATH_SEPARATOR ) );

	private final Identifier identifier = new Identifier();

	private String tableName;

	/**
	 * Holds the properties of this entity. Embedded properties are keyed by dot-separated path names.
	 */
	private final Map<String, Object> properties = new HashMap<String, Object>();

	CouchDBEntity() {
	}

	public CouchDBEntity(EntityKey key) {
		setId( identifier.createEntityId( key ) );
		tableName = key.getTable();
	}

	public void update(Tuple tuple) {
		for ( String columnName : tuple.getColumnNames() ) {
			properties.put( columnName, tuple.get( columnName ) );
		}
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@JsonIgnore
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Returns a map with all non-static properties. Will contain nested maps in case of embedded objects. Invoked by
	 * Jackson during serialization.
	 *
	 * @return a map with all non-static properties
	 */
	@JsonAnyGetter
	public Map<String, Object> getPropertiesAsHierarchy() {
		Map<String, Object> hierarchicalProperties = new HashMap<String, Object>();
		for ( Entry<String, Object> entry : properties.entrySet() ) {
			String columnName = entry.getKey();

			if ( isEmbeddedProperty( columnName ) ) {
				putEmbeddedProperty( hierarchicalProperties, columnName, entry.getValue() );
			}
			else {
				hierarchicalProperties.put( columnName, entry.getValue() );
			}
		}

		return hierarchicalProperties;
	}

	/**
	 * Adds the given embedded property indirectly to the given map, creating any intermediary embedded maps as required.
	 *
	 * @param root the root map to which the embedded property will be added
	 * @param name the dot-separated path denoting the property to add
	 * @param value the value of the property
	 */
	private void putEmbeddedProperty(Map<String, Object> root, String name, Object value) {
		String[] pathElements = PATH_SPLIT_PATTERN.split( name );

		Map<String, Object> owner = root;

		for ( int i = 0; i < pathElements.length - 1; i++ ) {
			String element = pathElements[i];

			@SuppressWarnings("unchecked")
			Map<String, Object> nextOwner = (Map<String, Object>) owner.get( element );
			if ( nextOwner == null ) {
				nextOwner = new HashMap<String, Object>();
				owner.put( element, nextOwner );
			}

			owner = nextOwner;
		}

		owner.put( pathElements[pathElements.length - 1], value );
	}

	private boolean isEmbeddedProperty(String columnName) {
		return columnName.contains( PATH_SEPARATOR );
	}

	/**
	 * Invoked by Jackson for any non-static property.
	 *
	 * @param name the property name
	 * @param value the property value
	 */
	@JsonAnySetter
	@SuppressWarnings("unchecked")
	public void set(String name, Object value) {
		if ( value instanceof Map ) {
			setMapValue( name, (Map<String, Object>) value );
		}
		properties.put( name, value );
	}

	private void setMapValue(String name, Map<String, Object> value) {
		for ( Entry<String, Object> entry : value.entrySet() ) {
			set( name + PATH_SEPARATOR + entry.getKey(), entry.getValue() );
		}
	}
}
