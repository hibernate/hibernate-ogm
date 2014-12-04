/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

import org.hibernate.ogm.model.spi.AssociationKind;

/**
 * Stores metadata information common to all keys related
  * to a given association
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface AssociationKeyMetadata {

	/**
	 * Returns the table name of this key.
	 *
	 * @return the table name of this key
	 */
	String getTable();

	/**
	 * The columns identifying the association.
	 *
	 * For example, in a many to many association, the row key will look like:
	 *
	 * <pre>
	 * RowKey{table='AccountOwner_BankAccount', columnNames=[owners_id, bankAccounts_id], columnValues=[...]},
	 * </pre>
	 *
	 * the association key will be something like:
	 *
	 * <pre>
	 * AssociationKey{table='AccountOwner_BankAccount', columnNames=[owners_id], columnValues=[...]},
	 * </pre>
	 *
	 * @return the columns names as an array, it never returns {@code null}
	 */
	String[] getColumnNames();

	/**
	 * The columns identifying an element of the association
	 *
	 * @return the column names identifying an element of the association
	 */
	String[] getRowKeyColumnNames();

	/**
	 * The columns representing the index of the element of the association.
	 * <p>
	 * For example, the key columns of a map-type property or the column with the order if the property is annotated with
	 * {@link javax.persistence.OrderColumn}
	 *
	 * @return the columns names representing the index of the element of the association
	 */
	String[] getRowKeyIndexColumnNames();

	/**
	 * Returns meta-data about the entity key referenced by associations of this key family.
	 *
	 * @return meta-data about the entity key referenced by associations of this key family.
	 */
	AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata();

	/**
	 * Returns all those columns from the given candidate list which are not part of this key family.
	 * <p>
	 * Stores can opt to persist only the returned columns when writing a row of this key family. All other columns can
	 * be retrieved from the key meta-data itself when reading an association row.
	 *
	 * @param candidates the column to check
	 * @return all the columns that are not part of this key family
	 */
	String[] getColumnsWithoutKeyColumns(Iterable<String> candidates);

	/**
	 * Returns the name of the single row key column which is not a column of this key itself, in case such a column
	 * exists.
	 * <p>
	 * If e.g. an association key contains the column "bankAccounts_id" and the row key columns are "bankAccounts_id"
	 * and "owners_id", this method will return "owners_id". But if the row columns were "bankAccounts_id", "owners_id"
	 * and "order", {@code null} would be returned as there were more than one column which are not part of the
	 * association key.
	 *
	 * @return the name of the single row key column which is not a column of this key itself or {@code null} if there
	 * is either no or more than one such column.
	 */
	String getSingleRowKeyColumnNotContainedInAssociationKey();

	/**
	 * Whether the given column is part of this key family or not.
	 *
	 * @param columnName the name of the column to check
	 * @return {@code true} if the given column is part of this key, {@code false} otherwise.
	 */
	boolean isKeyColumn(String columnName);

	/**
	 * Whether this key meta-data represents the inverse side of a bi-directional association.
	 *
	 * @return {@code true} if this key meta-data represents the inverse side of a bi-directional association,
	 * {@code false} otherwise.
	 */
	boolean isInverse();

	/**
	 * Get the association role.
	 *
	 * @return the association role
	 */
	String getCollectionRole();

	/**
	 * Get the type of association
	 *
	 * @return the association kind
	 */
	AssociationKind getAssociationKind();

	/**
	 * Check if the key identify a one-to-one association
	 *
	 * @return {@code true} if the association is one-to-one, {@code false} otherwise
	 */
	boolean isOneToOne();
}
