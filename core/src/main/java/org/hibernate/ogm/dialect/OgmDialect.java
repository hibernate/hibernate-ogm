/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import org.hibernate.dialect.Dialect;

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

	public GridDialect getGridDialect() {
		return gridDialect;
	}
}
