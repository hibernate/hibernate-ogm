/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.utils;

import org.hibernate.ogm.dialect.spi.GridDialect;

public class SkipByHelper {

	public static boolean isCurrentGridDialect(GridDialectType requiredType) {
		Class<? extends GridDialect> currentGridDialectClass = TestHelper.getCurrentGridDialectClass();
		boolean isCurrentGridDialect = false;
		if ( requiredType != null && requiredType.loadGridDialectClass() != null ) {
			isCurrentGridDialect = ( requiredType.loadGridDialectClass().equals( currentGridDialectClass ) );
		}
		return isCurrentGridDialect;
	}
}
