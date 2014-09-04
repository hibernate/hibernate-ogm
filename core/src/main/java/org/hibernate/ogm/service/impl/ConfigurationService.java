/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.Service;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ConfigurationService implements Service {

	private final boolean isOn;

	public ConfigurationService(Map config) {
		isOn = new ConfigurationPropertyReader( config )
			.property( InternalProperties.OGM_ON, boolean.class )
			.withDefault( false )
			.getValue();
	}

	public boolean isOgmOn() {
		return isOn;
	}
}
