/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.value;

import java.util.Map;

/**
 * Entity stored using a Redis Hash.
 *
 * @author Mark Paluch
 */
public class HashEntity extends StructuredValue {
	private final Map<String, String> entity;

	public HashEntity(Map<String, String> entity) {
		this.entity = entity;
	}

	public Map<String, String> getEntity() {
		return entity;
	}
}
