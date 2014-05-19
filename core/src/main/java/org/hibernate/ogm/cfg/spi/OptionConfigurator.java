/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.spi;

import org.hibernate.ogm.cfg.Configurable;

/**
 * A callback invoked at bootstrap time to apply configuration options. Can be passed via the option
 * {@link org.hibernate.ogm.cfg.OgmProperties#OPTION_CONFIGURATOR}.
 *
 * @author Gunnar Morling
 */
public abstract class OptionConfigurator {

	/**
	 * Callback for applying configuration options.
	 *
	 * @param configurable allows to apply store-specific configuration options
	 */
	public abstract void configure(Configurable configurable);
}
