/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Seiya Kawashima &lt;skawashima@uchicago.edu&gt;
 */
public class RedisHashTupleSnapshot implements TupleSnapshot {

	private final Map<String, Object> map;

	private SnapshotType snapshotType = SnapshotType.UNKNOWN;

	public RedisHashTupleSnapshot(Map<String, Object> map) {
		this.map = map;
	}

	@Override
	public Object get(String column) {
		return map.get( column );
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return map.keySet();
	}

	@Override
	public SnapshotType getSnapshotType() {
		return snapshotType;
	}

	@Override
	public void setSnapshotType(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
	}

	public Map<String, Object> getMap() {
		return map;
	}
}
