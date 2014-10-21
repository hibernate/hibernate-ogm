/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class TupleContextImpl implements TupleContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;
	private final OperationsQueue operationsQueue;

	/**
	 * Information of the associated entity stored per foreign key column names
	 */
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata;

	private final Map<String, String> roles;

	public TupleContextImpl(TupleContextImpl original, OperationsQueue operationsQueue) {
		this( original.selectableColumns, original.associatedEntityMetadata, original.roles, original.optionsContext, operationsQueue );
	}

	public TupleContextImpl(List<String> selectableColumns, Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata, Map<String, String> roles, OptionsContext optionsContext) {
		this( selectableColumns, associatedEntityMetadata, roles, optionsContext, null );
	}

	private TupleContextImpl(List<String> selectableColumns, Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata, Map<String, String> roles, OptionsContext optionsContext, OperationsQueue operationsQueue) {
		this.selectableColumns = selectableColumns;
		this.associatedEntityMetadata = Collections.unmodifiableMap( associatedEntityMetadata );
		this.roles = Collections.unmodifiableMap( roles );
		this.optionsContext = optionsContext;
		this.operationsQueue = operationsQueue;
	}

	@Override
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	@Override
	public boolean isPartOfAssociation(String column) {
		return associatedEntityMetadata.containsKey( column );
	}

	@Override
	public AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata(String column) {
		return associatedEntityMetadata.get( column );
	}

	@Override
	public Map<String, AssociatedEntityKeyMetadata> getAllAssociatedEntityKeyMetadata() {
		return associatedEntityMetadata;
	}

	@Override
	public String getRole(String column) {
		return roles.get( column );
	}

	@Override
	public Map<String, String> getAllRoles() {
		return roles;
	}

	@Override
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
