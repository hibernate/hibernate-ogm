/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

/**
 * List of log categories used by Hibernate OGM core module.
 * under impl package as it's still a work in progress.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public enum CoreLogCategories {
	DATASTORE_ACCESS("org.hibernate.ogm.datastore.access");

	private String category;

	CoreLogCategories(String name) {
		this.category = name;
	}

	@Override
	public String toString() {
		return category;
	}
}
