/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class TupleContext implements GridDialectOperationContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;
	private final OperationsQueue operationsQueue;

	/**
	 * Information of the associated entity stored per foreign key column names
	 */
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata;

	private final Map<String, String> roles;

	public TupleContext(TupleContext original, OperationsQueue operationsQueue) {
		this( original.selectableColumns, original.associatedEntityMetadata, original.roles, original.optionsContext, operationsQueue );
	}

	public TupleContext(List<String> selectableColumns, Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata, Map<String, String> roles, OptionsContext optionsContext) {
		this( selectableColumns, associatedEntityMetadata, roles, optionsContext, null );
	}

	private TupleContext(List<String> selectableColumns, Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata, Map<String, String> roles, OptionsContext optionsContext, OperationsQueue operationsQueue) {
		this.selectableColumns = selectableColumns;
		this.associatedEntityMetadata = Collections.unmodifiableMap( associatedEntityMetadata );
		this.roles = Collections.unmodifiableMap( roles );
		this.optionsContext = optionsContext;
		this.operationsQueue = operationsQueue;
	}

	/**
	 * Returns the mapped columns of the given entity. May be used by a dialect to only load those columns instead of
	 * the complete document/record.
	 */
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	/**
	 * Whether the given column is part of a *-to-one association or not. If so, a dialect may choose to not persist the
	 * column value in the corresponding tuple data structure itself but e.g. as a native relationship (in the case of
	 * graph stores).
	 */
	public boolean isPartOfAssociation(String column) {
		return associatedEntityMetadata.containsKey( column );
	}

	/**
	 * Provides meta-data about the *-to-one associations represented in a given tuple. Note that the same meta-data
	 * object will be returned for different columns, if those columns are part of a compound key.
	 *
	 * @param column The column name to return the *-to-one association meta-data for.
	 * @return meta-data about the *-to-one association of which the given column is part of or {@code null} if the
	 * given column is not part of such an association
	 */
	public AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata(String column) {
		return associatedEntityMetadata.get( column );
	}

	/**
	 * Returns meta-data about all the *-to-one associations represented in a given tuple, keyed by column name.
	 */
	public Map<String, AssociatedEntityKeyMetadata> getAllAssociatedEntityKeyMetadata() {
		return associatedEntityMetadata;
	}

	public String getRole(String column) {
		return roles.get( column );
	}

	public Map<String, String> getAllRoles() {
		return roles;
	}

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Tuple Context {" );

		builder.append( StringHelper.join( selectableColumns, ", " ) );
		builder.append( "}" );

		return builder.toString();
	}
}
