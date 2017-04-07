/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.options.impl;

import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Specifies that objects in collections are loaded from parent objects's node only.
 *
 * @author Victor Kadachigov
 */
public class CollocatedAssociationOption extends UniqueOption<Boolean> {

	@Override
	public Boolean getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return false;
	}
}
