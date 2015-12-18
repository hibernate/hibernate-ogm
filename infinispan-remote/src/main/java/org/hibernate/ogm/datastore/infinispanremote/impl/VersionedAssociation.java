/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationSnapshot;


public class VersionedAssociation extends Association {

	private long version;

	public VersionedAssociation() {
		super();
	}

	public VersionedAssociation(AssociationSnapshot snapshot) {
		super( snapshot );
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

}
