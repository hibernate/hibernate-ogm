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
package org.hibernate.ogm.dialect.couchdb.model;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.dialect.couchdb.backend.json.Document;
import org.hibernate.ogm.dialect.couchdb.backend.json.EntityDocument;

/**
 * A {@link CouchDBAssociation} backed by an {@link EntityDocument}.
 *
 * @author Gunnar Morling
 */
class EmbeddedAssociation extends CouchDBAssociation {

	private final EntityDocument entity;
	private final String name;

	public EmbeddedAssociation(EntityDocument entity, String name) {
		this.entity = entity;
		this.name = name;
	}

	@Override
	public List<Map<String, Object>> getRows() {
		return entity.getAssociation( name );
	}

	@Override
	public void setRows(List<Map<String, Object>> rows) {
		entity.setAssociation( name, rows );
	}

	@Override
	public Document getOwningDocument() {
		return entity;
	}
}
