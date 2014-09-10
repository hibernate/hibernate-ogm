/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.association.spi;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRow.AssociationRowAccessor;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Base class for {@link AssociationRowFactory} implementations which support association rows stored as key/value
 * tuples as well as rows stored as collections of single values.
 * This opening the way to optimise the storage and remove the column name from the structure for collections of
 * single values.
 * <p>
 * The single value form may be used to persist association rows with exactly one column (which is the case for collections of
 * simple values such as {@code int}s, {@code String} s etc. as well as associations based on non-composite keys). In
 * this case a row object of type {@code R} will be created using the column value and the single row key column which
 * is not part of the association key.
 * <p>
 * For rows with more than one column it is assumed that they are already of type {@code R} and they are thus passed
 * through as is.
 *
 * @author Gunnar Morling
 * @param <R> The type of key/value association rows supported by this factory.
 */
public abstract class SingleColumnAwareAssociationRowFactory<R> implements AssociationRowFactory {

	/**
	 * The type of key/value association rows supported by this factory; This basically corresponds to {@code Class<R>}
	 * but this form is used to support parameterized types such as {@code Map}.
	 */
	private final Class<?> associationRowType;

	protected SingleColumnAwareAssociationRowFactory(Class<?> associationRowType) {
		this.associationRowType = associationRowType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AssociationRow<?> createAssociationRow(AssociationKey associationKey, Object row) {
		R rowObject = null;

		if ( associationRowType.isInstance( row ) ) {
			rowObject = (R) row;
		}
		else {
			String columnName = associationKey.getMetadata().getSingleRowKeyColumnNotContainedInAssociationKey();
			Contracts.assertNotNull( columnName, "columnName" );
			rowObject = getSingleColumnRow( columnName, row );
		}

		return new AssociationRow<R>( associationKey, getAssociationRowAccessor(), rowObject );
	}

	/**
	 * Creates a row object with the given column name and value.
	 */
	protected abstract R getSingleColumnRow(String columnName, Object value);

	/**
	 * Returns the {@link AssociationRowAccessor} to be used to obtain values from the {@link AssociationRow}
	 * created by this factory.
	 */
	protected abstract AssociationRowAccessor<R> getAssociationRowAccessor();
}
