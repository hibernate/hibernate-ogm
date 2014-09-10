/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Represents an empty {@link AssociationSnapshot}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public final class EmptyAssociationSnapshot implements AssociationSnapshot {

	public static final EmptyAssociationSnapshot INSTANCE = new EmptyAssociationSnapshot();

	private EmptyAssociationSnapshot() {
	}

	@Override
	public Tuple get(RowKey column) {
		return null;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return false;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return Collections.emptySet();
	}

}
