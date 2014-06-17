/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import org.hibernate.ogm.datastore.document.association.spi.KeyedAssociationRows;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.grid.AssociationKey;

/**
 * {@link AssociationSnapshot} implementation based on a
 * {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument} object as written to and retrieved from
 * the CouchDB server.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
public class CouchDBAssociationSnapshot extends KeyedAssociationRows {

	/**
	 * The original association representing this snapshot as retrieved from CouchDB.
	 */
	private final CouchDBAssociation couchDbAssociation;

	public CouchDBAssociationSnapshot(CouchDBAssociation association, AssociationKey key) {
		super( key, association.getRows(), CouchDBAssociationRowFactory.INSTANCE );
		this.couchDbAssociation = association;
	}

	public CouchDBAssociation getCouchDbAssociation() {
		return couchDbAssociation;
	}
}
