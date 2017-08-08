/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Provides context information related to the tuple type to {@link GridDialect}s when accessing
 * {@link Tuple}s.
 *
 * @author Guillaume Smet
 */
public interface TupleTypeContext {

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
	 * Additional columns that are not part of the entity but that are added by subclasses in a hierarchy.
	 * <p>
	 * A dialect can use this extra information to get the additional columns if it doesn't know in advance the type of
	 * entity mapping the tuple, for example in query like FROM {@code From Person p} where Person is the parent type in a
	 * hierarchy.
	 */
	Set<String> getPolymorphicEntityColumns();

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
	 * Get the meta-data of all the associated entities keys
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
	 * @return the roles, keyed by column name.
	 */
	Map<String, String> getAllRoles();

	/**
	 * Get the name of the column with the discriminator.
	 *
	 * @return the name of the column or null if the discriminator is not column based
	 */
	String getDiscriminatorColumn();

	/**
	 * The discriminator value for dealing with inheritance; it might be {@code null} because some strategies don't need
	 * it.
	 *
	 * @return the value of the discriminator. It can return {@code null}.
	 */
	Object getDiscriminatorValue();
}
