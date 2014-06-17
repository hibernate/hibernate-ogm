/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.List;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;

/**
 * Represents an association stored in CouchDB, backed either by an association document (external storage of
 * associations) or an association sub-tree within an entity document (embedded storage of associations).
 * <p>
 * The owning document must be written back to CouchDB to make changes to the rows of an association persistent in the
 * data store.
 *
 * @author Gunnar Morling
 */
public abstract class CouchDBAssociation {

	/**
	 * Creates a {@link CouchDBAssociation} from the given {@link EntityDocument} and association name.
	 */
	public static CouchDBAssociation fromEmbeddedAssociation(EntityDocument entity, String name) {
		return new EmbeddedAssociation( entity, name );
	}

	/**
	 * Creates a {@link CouchDBAssociation} from the given {@link AssociationDocument}.
	 */
	public static CouchDBAssociation fromAssociationDocument(AssociationDocument associationDocument) {
		return new DocumentBasedAssociation( associationDocument );
	}

	/**
	 * Returns a list with all the rows of this association. Does not contain columns which are part of the association
	 * key.
	 */
	public abstract List<Object> getRows();

	/**
	 * Sets the rows of this association. The given list must not contain columns which are part of the association key.
	 */
	public abstract void setRows(List<Object> rows);

	/**
	 * Returns the CouchDB document which owns this association, either an {@link AssociationDocument} or an
	 * {@link EntityDocument}.
	 */
	public abstract Document getOwningDocument();
}
