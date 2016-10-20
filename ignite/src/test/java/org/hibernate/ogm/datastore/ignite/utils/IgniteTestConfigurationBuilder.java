/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils;

import java.net.URL;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;

public class IgniteTestConfigurationBuilder {


	public IgniteConfiguration build() {

		IgniteConfiguration config = null;

		try {
			config = loadFromResource( "ignite-config.xml" );
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return config;
	}

	private IgniteConfiguration loadFromResource(String resourceName) throws IgniteCheckedException {
		URL url = getClass().getClassLoader().getResource( resourceName );
		return IgnitionEx.loadConfiguration( url ).get1();
	}
}
