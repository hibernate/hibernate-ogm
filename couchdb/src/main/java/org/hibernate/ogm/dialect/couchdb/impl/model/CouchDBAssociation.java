/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.couchdb.impl.model;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.couchdb.impl.backend.json.AssociationDocument;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.Document;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.EntityDocument;

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
	public abstract List<Map<String, Object>> getRows();

	/**
	 * Sets the rows of this association. The given list must not contain columns which are part of the association key.
	 */
	public abstract void setRows(List<Map<String, Object>> rows);

	/**
	 * Returns the CouchDB document which owns this association, either an {@link AssociationDocument} or an
	 * {@link EntityDocument}.
	 */
	public abstract Document getOwningDocument();
}
