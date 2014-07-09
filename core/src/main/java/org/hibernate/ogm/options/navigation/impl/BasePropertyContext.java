/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.PropertyContext;

/**
 * Base implementation for {@link PropertyContext}s.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public abstract class BasePropertyContext<E extends EntityContext<E, P>, P extends PropertyContext<E, P>> extends BaseContext implements PropertyContext<E, P> {

	public BasePropertyContext(ConfigurationContext context) {
		super( context );
	}

}
