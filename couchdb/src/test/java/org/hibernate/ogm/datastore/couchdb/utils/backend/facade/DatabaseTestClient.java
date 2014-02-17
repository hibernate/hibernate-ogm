/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.couchdb.utils.backend.facade;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.couchdb.utils.backend.json.AssociationCountResponse;
import org.hibernate.ogm.datastore.couchdb.utils.backend.json.EntityCountResponse;
import org.hibernate.ogm.datastore.couchdb.utils.backend.json.designdocument.AssociationsDesignDocument;
import org.hibernate.ogm.datastore.couchdb.utils.backend.json.designdocument.EntitiesDesignDocument;

/**
 * Provides access to test-only APIs for a given CouchDB instance.
 *
 * @author Gunnar Morling
 */
@Path("/")
@Produces("application/json")
@Consumes("application/json")
public interface DatabaseTestClient {

	/**
	 * Retrieves the number of associations stored in the database.
	 *
	 * @param group must always be set to {@code true}; can't be avoided unfortunately as JAX-RS doesn't allow to
	 * specify fixed parameters in another way
	 * @return the response in form of an {@link AssociationCountResponse} entity
	 * @see AssociationCountResponse
	 */
	@GET
	@Path(AssociationsDesignDocument.ASSOCIATION_COUNT_PATH)
	Response getNumberOfAssociations(@QueryParam("group") boolean group);

	/**
	 * Retrieves the number of entities stored in the database.
	 *
	 * @return the response in form of an {@link EntityCountResponse}
	 * @see EntityCountResponse
	 */
	@GET
	@Path(EntitiesDesignDocument.ENTITY_COUNT_PATH)
	Response getNumberOfEntities();
}
