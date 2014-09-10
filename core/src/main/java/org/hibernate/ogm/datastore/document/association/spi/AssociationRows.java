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

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Represents the rows of an association in form of {@link AssociationRow}s.
 *
 * @author Gunnar Morling
 */
public class AssociationRows implements AssociationSnapshot {

	private final Map<RowKey, AssociationRow<?>> rows;

	public AssociationRows(AssociationKey associationKey, Collection<?> wrapped, AssociationRowFactory associationRowFactory) {
		this.rows = newHashMap( wrapped.size() );

		for ( Object object : wrapped ) {
			AssociationRow<?> row = associationRowFactory.createAssociationRow( associationKey, object );
			rows.put( row.getKey(), row );
		}
	}

	@Override
	public Tuple get(RowKey rowKey) {
		AssociationRow<?> row = rows.get( rowKey );
		return row != null ? new Tuple( row ) : null;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		return rows.containsKey( rowKey );
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
