/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.association.spi;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * Represents the rows of an association in form of {@link KeyedAssociationRow}s.
 *
 * @author Gunnar Morling
 */
public class KeyedAssociationRows implements AssociationSnapshot {

	private final Map<RowKey, KeyedAssociationRow<?>> rows;

	public KeyedAssociationRows(AssociationKey associationKey, Collection<?> wrapped, AssociationRowFactory associationRowFactory) {
		this.rows = newHashMap( wrapped.size() );

		for ( Object object : wrapped ) {
			KeyedAssociationRow<?> row = associationRowFactory.createAssociationRow( associationKey, object );
			rows.put( row.getKey(), row );
		}
	}

	@Override
	public Tuple get(RowKey column) {
		KeyedAssociationRow<?> row = rows.get( column );
		return row != null ? new Tuple( row ) : null;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return rows.containsKey( column );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return rows.keySet();
	}

	@Override
	public int size() {
		return rows.size();
	}
}
