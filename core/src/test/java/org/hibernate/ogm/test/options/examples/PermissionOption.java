/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.examples;

import org.hibernate.ogm.options.spi.Option;

/**
 * An example non-unique option for testing purposes.
 *
 * @author Gunnar Morling
 *
 */
public class PermissionOption extends Option<String, String> {

	private final String role;

	public PermissionOption(String role) {
		this.role = role;
	}

	@Override
	public String getOptionIdentifier() {
		return role;
	}
}
