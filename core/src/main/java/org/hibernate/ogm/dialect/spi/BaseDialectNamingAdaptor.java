/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

public class BaseDialectNamingAdaptor implements DialectNamingAdaptor {

	static final DialectNamingAdaptor DEFAULT_INSTANCE = new BaseDialectNamingAdaptor();

	public BaseDialectNamingAdaptor() {
	}

	@Override
	public String makeValidTableName(String requestedName) {
		return requestedName;
	}

}
