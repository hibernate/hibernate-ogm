/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.Set;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * A {@link TupleSnapshot} based on a CouchDB {@link EntityDocument}.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class CouchDBTupleSnapshot implements TupleSnapshot {

	private final EntityDocument entity;
	private SnapshotType snapshotType;

	public CouchDBTupleSnapshot(EntityDocument entity, SnapshotType snapshotType) {
		this.entity = entity;
		this.snapshotType = snapshotType;
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

	@Override
	public SnapshotType getSnapshotType() {
		return snapshotType;
	}

	@Override
	public void setSnapshotType(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;
	}

	public EntityDocument getEntity() {
		return entity;
	}
}
