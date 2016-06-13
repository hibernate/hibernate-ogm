/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Provides context information related to the association type to {@link GridDialect}s when accessing
 * {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public interface AssociationTypeContext {

	/**
	 * Provide access to the options set for the association.
	 *
	 * @return a context object providing access to the options effectively applying for the association.
	 */
	OptionsContext getOptionsContext();

	/**
	 * Provide access to the options set for the entity owner of the association.
	 *
	 * @return a context object providing access to the options effectively applying for the owner entity.
	 */
	OptionsContext getOwnerEntityOptionsContext();

	/**
	 * Provides meta-data about the entity key on the other side of this association.
	 *
	 * @return A meta-data object providing information about the entity key on the other side of this information.
	 */
	AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata();

	/**
	 * Provides the role of the represented association on the main side in case the current operation is invoked for
	 * the inverse side of a bi-directional association.
	 *
	 * @return The role of the represented association on the main side. The association's own role will be returned in
	 * case this operation is invoked for an uni-directional association or the main-side of a bi-directional
	 * association.
	 */
	String getRoleOnMainSide();
}
