/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.couchdb.util.impl.Identifier;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Contains the information related to a {@link Tuple}. The use of this class is to serialize and deserialize the JSON
 * stored in CouchDB; Documents have the following structure:
 *
 * <pre>
 * {@code
 * {
 *     "_id": "a4jdefe8",
 *     "_rev": "123",
 *     "$type": "entity",
 *     "$table": "Foo",
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
 * Implementation note: The entity's properties are stored in a map, with embedded properties being represented by
 * dot-separated property paths. When (de-)serializing this document from/to JSON via Jackson, this flat representation
 * is converted into a hierarchical representation using nested maps (if embedded properties are present).
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeName(EntityDocument.TYPE_NAME)
public class EntityDocument extends Document {

	/**
	 * The name of this document type as materialized in {@link Document#TYPE_DISCRIMINATOR_FIELD_NAME}.
	 */
	public static final String TYPE_NAME = "entity";

	/**
	 * Name of the table discriminator field
	 */
	private static final String TABLE_FIELD_NAME = "$table";
	private static final String PATH_SEPARATOR = ".";
	private static final Pattern PATH_SPLIT_PATTERN = Pattern.compile( Pattern.quote( PATH_SEPARATOR ) );

	private String table;

	/**
	 * Holds the properties of this entity. Embedded properties are keyed by dot-separated path names.
	 */
	private final Map<String, Object> properties = new HashMap<String, Object>();

	EntityDocument() {
	}

	public EntityDocument(EntityKey key) {
		this( key, null, null );
	}

	/**
	 * Creates a new entity representing the given tuple.
	 *
	 * @param key of the entity
	 * @param revision the revision of the entity when loaded; may be {@code null} when inserting a new entity
	 * @param tuple the properties of the entity; may be {@code null} when inserting a new entity
	 */
	public EntityDocument(EntityKey key, String revision, Tuple tuple) {
		super( Identifier.createEntityId( key ), revision );
		table = key.getTable();

		if ( tuple != null ) {
			for ( String columnName : tuple.getColumnNames() ) {
				if ( columnName != Document.REVISION_FIELD_NAME ) {
					properties.put( columnName, tuple.get( columnName ) );
				}
			}
		}
	}

	@JsonProperty(TABLE_FIELD_NAME)
	public String getTable() {
		return table;
	}

	@JsonProperty(TABLE_FIELD_NAME)
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * Returns all properties of this entity, including its revision.
	 *
	 * @return all properties of this entity
	 */
	@JsonIgnore
	public Map<String, Object> getProperties() {
		Map<String, Object> props = new HashMap<String, Object>( properties );

		if ( getRevision() != null ) {
			props.put( Document.REVISION_FIELD_NAME, getRevision() );
		}

		return props;
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
	public static void putEmbeddedProperty(Map<String, Object> root, String name, Object value) {
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

	public static boolean isEmbeddedProperty(String columnName) {
		return columnName.contains( PATH_SEPARATOR );
	}

	/**
	 * Invoked by Jackson for any non-static property.
	 * <p>
	 * A {@link Map} creates an additional set of properties, one for each entry of the map.
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
		else {
			properties.put( name, value );
		}
	}

	/**
	 * Saves each entry of the map as a single property using the path separator.
	 * <p>
	 * For example { k1 = { k11 = v11, k12 = v12 } } becomes { k1.k11=v11, k1.k12=v12 }.
	 */
	private void setMapValue(String name, Map<String, Object> value) {
		for ( Entry<String, Object> entry : value.entrySet() ) {
			set( name + PATH_SEPARATOR + entry.getKey(), entry.getValue() );
		}
	}

	public List<Object> getAssociation(String name) {
		@SuppressWarnings("unchecked")
		List<Object> association = (List<Object>) properties.get( name );
		return association != null ? association : Collections.<Object>emptyList();
	}

	@JsonIgnore
	public void setAssociation(String name, List<Object> rows) {
		properties.put( name, rows );
	}

	@JsonIgnore
	public void removeAssociation(String name) {
		properties.remove( name );
	}
}
