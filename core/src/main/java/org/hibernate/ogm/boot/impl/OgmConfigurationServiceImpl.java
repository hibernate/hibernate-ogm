/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import org.hibernate.ogm.service.impl.OgmConfigurationService;

public class OgmConfigurationServiceImpl implements OgmConfigurationService {

	private final boolean isEnabled;

	public OgmConfigurationServiceImpl(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	@Override
	public boolean isOgmEnabled() {
		return isEnabled;
	}
}
