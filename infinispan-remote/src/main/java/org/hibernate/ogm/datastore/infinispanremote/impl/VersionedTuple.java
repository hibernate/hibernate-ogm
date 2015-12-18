/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleSnapshot;

public final class VersionedTuple extends Tuple {

	private long version;
	private final boolean beingInserted;

	public VersionedTuple(boolean beingInserted) {
		super();
		this.beingInserted = beingInserted;
	}

	public VersionedTuple(TupleSnapshot snapshot) {
		super( snapshot );
		this.beingInserted = false;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public boolean isBeingInserted() {
		return beingInserted;
	}

}
