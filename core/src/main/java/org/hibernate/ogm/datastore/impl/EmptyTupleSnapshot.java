/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class EmptyTupleSnapshot implements TupleSnapshot {

	public static final TupleSnapshot INSTANCE = new EmptyTupleSnapshot();

	private EmptyTupleSnapshot() {
	}

	@Override
	public Object get(String column) {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public Set<String> getColumnNames() {
		return Collections.emptySet();
	}
}
