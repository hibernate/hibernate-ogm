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
import java.util.Set;

import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class TupleTypeContextImpl implements TupleTypeContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;

	private final String discriminatorColumn;
	private final Object discriminatorValue;

	/**
	 * Information of the associated entity stored per foreign key column names
	 */
	private final Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata;

	private final Map<String, String> roles;
	private final Set<String> polymorphicEntityColumns;

	public TupleTypeContextImpl(List<String> selectableColumns,
			Set<String> polymorphicEntityColumns,
			Map<String, AssociatedEntityKeyMetadata> associatedEntityMetadata,
			Map<String, String> roles,
			OptionsContext optionsContext,
			String discriminatorColumn,
			Object discriminatorValue) {

		this.polymorphicEntityColumns = Collections.unmodifiableSet( polymorphicEntityColumns );
		this.selectableColumns = Collections.unmodifiableList( selectableColumns );
		this.associatedEntityMetadata = Collections.unmodifiableMap( associatedEntityMetadata );
		this.roles = Collections.unmodifiableMap( roles );
		this.optionsContext = optionsContext;
		this.discriminatorColumn = discriminatorColumn;
		this.discriminatorValue = discriminatorValue;
	}

	@Override
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public Set<String> getPolymorphicEntityColumns() {
		return polymorphicEntityColumns;
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
	public String getDiscriminatorColumn() {
		return discriminatorColumn;
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Tuple Context {" );

		builder.append( "selectableColumns: [" );
		builder.append( StringHelper.join( selectableColumns, ", " ) );
		builder.append( "] }" );

		return builder.toString();
	}
}
