/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * A {@link TupleSnapshot} based on the properties of a CouchDB {@link EntityDocument}.
 * <p>
 * Fundamentally a {@link MapTupleSnapshot} except that the {@link EntityKey} column names and values are copied.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class CouchDBTupleSnapshot implements TupleSnapshot {

	private final Map<String, Object> properties;
	private final boolean createdOnInsert;

	public CouchDBTupleSnapshot(EntityKey key) {
		createdOnInsert = true;

		properties = new HashMap<String, Object>();
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			properties.put( key.getColumnNames()[i], key.getColumnValues()[i] );
		}
	}

	public CouchDBTupleSnapshot(Map<String, Object> properties) {
		createdOnInsert = false;
		this.properties = properties;
	}

	@Override
	public Object get(String column) {
		return properties.get( column );
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return properties.keySet();
	}

	/**
	 * Whether this snapshot has been created during an insert or not.
	 *
	 * @return {@code true} if the snapshot has been created during an insert, {@code false} if it has been created
	 * during an update.
	 */
	public boolean isCreatedOnInsert() {
		return createdOnInsert;
	}
}
