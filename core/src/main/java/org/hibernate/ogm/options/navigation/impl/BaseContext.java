/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.options.spi.Option;

/**
 * Base class for {@link org.hibernate.ogm.options.navigation.GlobalContext},
 * {@link org.hibernate.ogm.options.navigation.EntityContext} and
 * {@link org.hibernate.ogm.options.navigation.PropertyContext} implementations which allows to add options for
 * the different kinds of context.
 *
 * @author Gunnar Morling
 */
public class BaseContext {

	private final ConfigurationContext context;

	public BaseContext(ConfigurationContext context) {
		this.context = context;
	}

	protected final <V> void addGlobalOption(Option<?, V> option, V value) {
		context.addGlobalOption( option, value );
	}

	protected final <V> void addEntityOption(Option<?, V> option, V value) {
		context.addEntityOption( option, value );
	}

	protected final <V> void addPropertyOption(Option<?, V> option, V value) {
		context.addPropertyOption( option, value );
	}
}
