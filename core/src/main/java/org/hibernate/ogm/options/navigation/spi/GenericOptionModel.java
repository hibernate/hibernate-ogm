/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.spi;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;

/**
 * A generic option model that provides no store-specific options.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class GenericOptionModel {

	public static GlobalContext<?, ?> createGlobalContext(ConfigurationContext context) {
		return context.createGlobalContext( GenericGlobalOptions.class, GenericEntityOptions.class, GenericPropertyOptions.class );
	}

	private abstract static class GenericGlobalOptions extends BaseGlobalContext<GenericGlobalOptions, GenericEntityOptions> implements
			GlobalContext<GenericGlobalOptions, GenericEntityOptions> {

		public GenericGlobalOptions(ConfigurationContext context) {
			super( context );
		}
	}

	private abstract static class GenericEntityOptions extends BaseEntityContext<GenericEntityOptions, GenericPropertyOptions> implements
			EntityContext<GenericEntityOptions, GenericPropertyOptions> {

		public GenericEntityOptions(ConfigurationContext context) {
			super( context );
		}
	}

	private abstract static class GenericPropertyOptions extends BasePropertyContext<GenericEntityOptions, GenericPropertyOptions> implements
			PropertyContext<GenericEntityOptions, GenericPropertyOptions> {

		public GenericPropertyOptions(ConfigurationContext context) {
			super( context );
		}
	}
}
