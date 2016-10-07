/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.value;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity stored using a Redis Hash.
 *
 * Note that with Redis hash, all the data types are encoded as strings.
 *
 * @author Mark Paluch
 */
public class HashEntity extends StructuredValue {
	private final Map<String, String> properties;

	public HashEntity(Map<String, String> properties) {
		this.properties = properties;
	}

	@JsonAnyGetter
	public Map<String, String> getProperties() {
		return properties;
	}

	@JsonAnySetter
	public void set(String name, String value) {
		properties.put( name, value );
	}

	@JsonIgnore
	public void unset(String name) {
		properties.remove( name );
	}

	@JsonIgnore
	public Object get(String column) {
		return properties.get( column );
	}

	@JsonIgnore
	public boolean has(String column) {
		return properties.containsKey( column );
	}

	@JsonIgnore
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@JsonIgnore
	public Set<String> getColumnNames() {
		return properties.keySet();
	}
}
