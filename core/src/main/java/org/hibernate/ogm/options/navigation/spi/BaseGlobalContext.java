/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.spi;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * Base implementation {@link GlobalContext}s.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public abstract class BaseGlobalContext<G extends GlobalContext<G, E>, E extends EntityContext<E, ?>> extends BaseContext implements GlobalContext<G, E> {

	public BaseGlobalContext(ConfigurationContext context) {
		super( context );
	}
}
