/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache;

import org.hibernate.ogm.cfg.OgmProperties;

/**
 * Properties for configuring the Ehcache datastore via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public final class EhcacheProperties implements OgmProperties {

	/**
	 * Configuration property for specifying the name of the Ehcache configuration file
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.ehcache.configuration_resource_name";

	private EhcacheProperties() {
	}
}
