/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
