/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.dialect.Dialect;
import org.hibernate.ogm.dialect.GridDialect;

/**
 * A pseudo {@link Dialect} implementation which exposes the current {@link GridDialect}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OgmDialect extends Dialect {

	private final GridDialect gridDialect;

	public OgmDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
	}

	/**
	 * Returns the current {@link GridDialect}.
	 * <p>
	 * Intended for usage in code interacting with ORM SPIs which only provide access to the {@link Dialect} but not the
	 * service registry. Other code should obtain the grid dialect from the service registry.
	 *
	 * @return the current grid dialect.
	 */
	public GridDialect getGridDialect() {
		return gridDialect;
	}
}
