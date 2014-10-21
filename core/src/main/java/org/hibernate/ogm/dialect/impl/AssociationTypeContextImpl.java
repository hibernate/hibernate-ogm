/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class AssociationTypeContextImpl implements AssociationTypeContext {

	private final OptionsContext optionsContext;
	private final AssociatedEntityKeyMetadata associatedEntityKeyMetadata;
	private final String roleOnMainSide;

	public AssociationTypeContextImpl(OptionsContext optionsContext, AssociatedEntityKeyMetadata associatedEntityKeyMetadata, String roleOnMainSide) {
		this.optionsContext = optionsContext;
		this.associatedEntityKeyMetadata = associatedEntityKeyMetadata;
		this.roleOnMainSide = roleOnMainSide;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	/**
	 * Provides meta-data about the entity key on the other side of this association.
	 *
	 * @return A meta-data object providing information about the entity key on the other side of this information.
	 */
	@Override
	public AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata() {
		return associatedEntityKeyMetadata;
	}

	/**
	 * Provides the role of the represented association on the main side in case the current operation is invoked for
	 * the inverse side of a bi-directional association.
	 *
	 * @return The role of the represented association on the main side. The association's own role will be returned in
	 * case this operation is invoked for an uni-directional association or the main-side of a bi-directional
	 * association.
	 */
	@Override
	public String getRoleOnMainSide() {
		return roleOnMainSide;
	}

	@Override
	public String toString() {
		return "AssociationContext [optionsContext=" + optionsContext + "]";
	}
}
