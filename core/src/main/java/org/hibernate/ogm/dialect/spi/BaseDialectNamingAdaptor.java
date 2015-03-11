/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.util.impl.Contracts;

/**
 * Base implementation of {@link DialectNamingAdaptor}.
 *
 * It's generally a good idea to extend this class to create a new {@link DialectNamingAdaptor} implementation,
 * however that's optional. The benefit of extending this class is that you won't have compilation errors
 * in case we add new methods to the contract of the {@link DialectNamingAdaptor} interface in future versions.
 *
 * @author Sanne Grinovero
 */
public class BaseDialectNamingAdaptor implements DialectNamingAdaptor {

	static final DialectNamingAdaptor DEFAULT_INSTANCE = new BaseDialectNamingAdaptor();

	protected BaseDialectNamingAdaptor() {
		// Shouldn't be needed to be invoked, except by extensions
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String makeValidTableName(String requestedName) {
		Contracts.assertStringParameterNotEmpty( requestedName, "requestedName" );
		return requestedName;
	}

}
