/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.association.spi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * A {@link TupleSnapshot} which represents one row of an association.
 * <p>
 * It obtains its values from the store-specific native representation of the association row as well as the association
 * key. Column values from both sources are exposed in a uniformed manner. It retains a reference to the store-specific
 * association row, retrieving values from it via the {@link AssociationRowAccessor} contract.
 * <p>
 * The key of the row can be retrieved via {@link #getKey()}, again its values are obtained from the association key if
 * present there or from the native association representation otherwise.
 *
 * @author Gunnar Morling
 */
public class AssociationRow<R> implements TupleSnapshot {

	/**
	 * Contract for obtaining association tuple values from the store-specific representation of an association row.
	 * Columns not present in the association key will be retrieved via this contract.
	 * <p>
	 * Implementations should be stateless and one single instance should be used to access all associations of a given
	 * datastore.
	 *
	 * @param <R> The store-specific type for representing association rows
	 */
	public interface AssociationRowAccessor<R> {

		Set<String> getColumnNames(R row);

		Object get(R row, String column);
	}

	private final AssociationKey associationKey;
	private final AssociationRowAccessor<R> accessor;
	private final R row;

	private final Set<String> columnNames;
	private final RowKey rowKey;

	public AssociationRow(AssociationKey associationKey, AssociationRowAccessor<R> accessor, R row) {
		this.associationKey = associationKey;
		this.accessor = accessor;
		this.row = row;

		this.columnNames = Collections.unmodifiableSet( buildColumnNames( associationKey, accessor.getColumnNames( row ) ) );
		this.rowKey = buildRowKey( associationKey, row, accessor );
	}

	private static Set<String> buildColumnNames(AssociationKey associationKey, Set<String> columnsFromRow) {
		Set<String> columnNames = new HashSet<String>( columnsFromRow.size() + associationKey.getColumnNames().length );
		columnNames.addAll( columnsFromRow );
		Collections.addAll( columnNames, associationKey.getColumnNames() );
		return columnNames;
	}

	/**
	 * Creates the row key of the given association row; columns present in the given association key will be obtained
	 * from there, all other columns from the given native association row.
	 */
	private static <R> RowKey buildRowKey(AssociationKey associationKey, R row, AssociationRowAccessor<R> accessor) {
		String[] columnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Object[] columnValues = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			String columnName = columnNames[i];
			columnValues[i] = associationKey.getMetadata().isKeyColumn( columnName ) ? associationKey.getColumnValue( columnName ) : accessor.get( row, columnName );
		}

		return new RowKey( columnNames, columnValues );
	}

	@Override
	public Object get(String column) {
		return associationKey.getMetadata().isKeyColumn( column ) ? associationKey.getColumnValue( column ) : accessor.get( row, column );
	}

	@Override
	public boolean isEmpty() {
		return columnNames.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return columnNames;
	}

	/**
	 * Returns the key of this association row.
	 *
	 * @return The key of this association row
	 */
	public RowKey getKey() {
		return rowKey;
	}
}
