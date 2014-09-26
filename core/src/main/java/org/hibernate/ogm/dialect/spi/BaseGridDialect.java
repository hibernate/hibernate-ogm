/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

/**
 * Recommended base class for {@link GridDialect} implementations.
 *
 * @author Gunnar Morling
 */
public abstract class BaseGridDialect implements GridDialect {

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}
}
