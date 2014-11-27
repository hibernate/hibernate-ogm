/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public interface TupleContext {

	/**
	 * Get the option context.
	 *
	 * @return a context object providing access to the options effectively applying for a given entity or property.
	 */
	OptionsContext getOptionsContext();

	/**
	 * Returns the mapped columns of the given entity. May be used by a dialect to only load those columns instead of
	 * the complete document/record. If the dialect supports the embedded storage of element collections and
	 * associations, the respective columns will be part of the returned list as well.
	 *
	 * @return the columns that can be selected on the given entity
	 */
	List<String> getSelectableColumns();

	/**
	 * Whether the given column is part of a *-to-one association or not. If so, a dialect may choose to not persist the
	 * column value in the corresponding tuple data structure itself but e.g. as a native relationship (in the case of
	 * graph stores).
	 *
	 * @param column the name of the column
	 * @return {@code true} if the given column is part of a *-to-one association, {@code false} otherwise.
	 */
	boolean isPartOfAssociation(String column);

	/**
	 * Provides meta-data about the *-to-one associations represented in a given tuple. Note that the same meta-data
	 * object will be returned for different columns, if those columns are part of a compound key.
	 *
	 * @param column The column name to return the *-to-one association meta-data for.
	 * @return meta-data about the *-to-one association of which the given column is part of or {@code null} if the
	 * given column is not part of such an association
	 */
	AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata(String column);

	/**
	 * Get the the meta-data of all the associated entities keys
	 *
	 * @return the meta-data about all the *-to-one associations represented in a given tuple, keyed by column name.
	 */
	Map<String, AssociatedEntityKeyMetadata> getAllAssociatedEntityKeyMetadata();

	/**
	 * Get the role of a column
	 *
	 * @param column the column name
	 * @return the role of the given column
	 */
	String getRole(String column);

	/**
	 * Get all the roles
	 *
	 * @return the the roles, keyed by column name.
	 */
	Map<String, String> getAllRoles();

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	OperationsQueue getOperationsQueue();
}
