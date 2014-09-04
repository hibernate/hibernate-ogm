/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.examples;

import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * An option that can be used to set the name of something.
 * <p>
 * This is a {@link UniqueOption} that can be used for testing.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NameExampleOption extends UniqueOption<String> {

	public static final String NAME_OPTION = "hibernate.ogm.test.options.name";

	@Override
	public String getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( NAME_OPTION, String.class ).getValue();
	}
}
