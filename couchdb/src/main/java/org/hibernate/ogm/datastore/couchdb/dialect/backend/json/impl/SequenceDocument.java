/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Used to serialize and deserialize sequence objects. A generic map with a single value is used for writing/reading the
 * value in order to make the property customizable as per the generator's configuration.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeName(SequenceDocument.TYPE_NAME)
public class SequenceDocument extends Document {

	/**
	 * The name of this document type as materialized in {@link Document#TYPE_DISCRIMINATOR_FIELD_NAME}.
	 */
	public static final String TYPE_NAME = "sequence";

	private final Map<String, Object> properties = newHashMap( 1 );
	private String valueProperty;

	// Only used by Jackson
	SequenceDocument() {
	}

	public SequenceDocument(String valueProperty, long initialValue) {
		this.valueProperty = valueProperty;
		properties.put( valueProperty, String.valueOf( initialValue ) );
	}

	@JsonIgnore
	public long getValue() {
		return Long.valueOf( (String) properties.get( valueProperty ) );
	}

	public void increase(int increment) {
		long value = getValue() + increment;
		properties.put( valueProperty, String.valueOf( value ) );
	}

	// Only used by Jackson

	@JsonAnyGetter
	Map<String, Object> getProperties() {
		return properties;
	}

	@JsonAnySetter
	void set(String name, Object value) {
		if ( valueProperty == null ) {
			valueProperty = name;
		}

		properties.put( name, value );
	}
}
