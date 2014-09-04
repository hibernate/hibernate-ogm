/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid.spi;

import java.util.Arrays;

import org.hibernate.ogm.datastore.spi.Tuple;

/**
 * Provides meta-data about a one-to-one/many-to-one association represented by one or more columns contained within a
 * {@link Tuple}.
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class AssociatedEntityKeyMetadata {

	private final String[] associationKeyColumns;
	private final EntityKeyMetadata entityKeyMetadata;

	public AssociatedEntityKeyMetadata(String[] associationKeyColumns, EntityKeyMetadata entityKeyMetadata) {
		this.associationKeyColumns = associationKeyColumns;
		this.entityKeyMetadata = entityKeyMetadata;
	}

	/**
	 * Returns the meta-data for the entity key on the other side of the represented association.
	 */
	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	/**
	 * Returns the key of the entity targeted by the represented association, retrieved from the given tuple.
	 *
	 * @param tuple the tuple from which to retrieve the referenced entity key
	 * @return the key of the entity targeted by the represented association
	 */
	public EntityKey getEntityKey(Tuple tuple) {
		Object[] columnValues = new Object[ associationKeyColumns.length];
		int i = 0;

		for ( String associationKeyColumn : associationKeyColumns ) {
			columnValues[i] = tuple.get( associationKeyColumn );
			i++;
		}

		return new EntityKey( entityKeyMetadata, columnValues );
	}

	/**
	 * Returns the entity key on the other side of association row represented by the given row key.
	 * <p>
	 * <b>Note:</b> May only be invoked if the row key actually contains all the columns making up that entity key.
	 * Specifically, it may <b>not</b> be invoked if the association has index columns (maps, ordered collections), as
	 * the entity key columns will not be part of the row key in this case.
	 */
	public EntityKey getEntityKey(RowKey rowKey) {
		Object[] columnValues = new Object[associationKeyColumns.length];
		int i = 0;

		for ( String associationKeyColumn : associationKeyColumns ) {
			columnValues[i] = rowKey.getColumnValue( associationKeyColumn );
			i++;
		}

		return new EntityKey( entityKeyMetadata, columnValues );
	}

	/**
	 * Returns the name of the column in the target entity key corresponding to the given association key column.
	 * <p>
	 * E.g. let there be an entity key comprising the columns {@code id.countryCode} and {@code id.sequenceNo} which is
	 * referenced by the columns {@code address_id.countryCode} and {@code address_id.sequenceNo}. When invoked for the
	 * column {@code address_id.countryCode}, this method will return {@code id.countryCode}.
	 *
	 * @param associationKeyColumn The name of the column in the association
	 * @return The name of the corresponding column in the referenced entity key
	 */
	public String getCorrespondingEntityKeyColumn(String associationKeyColumn) {
		int i = 0;
		for ( String column : associationKeyColumns ) {
			if ( associationKeyColumn.equals( column ) ) {
				return entityKeyMetadata.getColumnNames()[i];
			}
			i++;
		}

		return null;
	}

	/**
	 * Returns the names of those columns of a tuple or association row which make up the represented association, i.e.
	 * the columns referring to the entity key on the other side.
	 */
	public String[] getAssociationKeyColumns() {
		return associationKeyColumns;
	}

	@Override
	public String toString() {
		return "AssociatedEntityKeyMetadata [associationKeyColumns=" + Arrays.toString( associationKeyColumns ) + ", entityKeyMetadata=" + entityKeyMetadata
				+ "]";
	}
}
