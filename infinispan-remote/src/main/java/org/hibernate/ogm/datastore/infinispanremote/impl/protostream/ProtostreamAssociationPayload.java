/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.util.Objects;

import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedAssociation;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;

public final class ProtostreamAssociationPayload {

	//One and only one of the following fields will be initialized:
	private final MapAssociationSnapshot loadedSnapshot;
	private final VersionedAssociation association;

	public ProtostreamAssociationPayload(MapAssociationSnapshot loadedSnapshot) {
		this.loadedSnapshot = Objects.requireNonNull( loadedSnapshot );
		this.association = null;
	}

	public ProtostreamAssociationPayload(VersionedAssociation tuple) {
		this.association = Objects.requireNonNull( tuple );
		this.loadedSnapshot = null;
	}

	public VersionedAssociation toAssociation() {
		if ( association != null ) {
			return association;
		}
		else {
			return new VersionedAssociation( loadedSnapshot );
		}
	}

}
