/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Set;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Seiya Kawashima &lt;skawashima@uchicago.edu&gt;
 */
public class RedisJsonTupleSnapshot implements TupleSnapshot {

	private final Entity entity;

	public RedisJsonTupleSnapshot(Entity entity) {
		this.entity = entity;
	}

	@Override
	public Object get(String column) {
		return entity.getProperty( column );
	}

	@Override
	public boolean isEmpty() {
		return entity.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return entity.getKeys();
	}

	public Entity getEntity() {
		return entity;
	}
}
