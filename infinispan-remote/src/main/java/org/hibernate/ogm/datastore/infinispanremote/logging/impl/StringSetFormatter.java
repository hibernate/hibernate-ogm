/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.logging.impl;

import java.util.Set;

public final class StringSetFormatter {

	private final Set<String> names;

	public StringSetFormatter(Set<String> names) {
		this.names = names;
	}

	@Override
	public String toString() {
		return names.toString();
	}

}
