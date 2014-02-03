/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.model.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.couchdb.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.dialect.couchdb.backend.json.impl.Document;

/**
 * A {@link CouchDBAssociation} backed by an {@link AssociationDocument}.
 *
 * @author Gunnar Morling
 */
class DocumentBasedAssociation extends CouchDBAssociation {

	private final AssociationDocument document;

	public DocumentBasedAssociation(AssociationDocument document) {
		this.document = document;
	}

	@Override
	public List<Map<String, Object>> getRows() {
		return document.getRows();
	}

	@Override
	public void setRows(List<Map<String, Object>> rows) {
		document.setRows( rows );
	}

	@Override
	public Document getOwningDocument() {
		return document;
	}
}
