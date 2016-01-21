/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

/**
 * @author Davide D'Alto
 */
public class TupleContextHelper {

	/**
	 * Given a {@link SessionImplementor} returns the {@link TupleContext} associated to an entity.
	 *
	 * @param session the current session
	 * @param metadata the {@link EntityMetadataInformation} of the entity associated to the TupleContext
	 * @return the TupleContext associated to the current session for the entity specified, or {@code null} if the
	 * EntityMetadataInformation is {@code null}
	 */
	public static TupleContext tupleContext(SessionImplementor session, EntityMetadataInformation metadata) {
		if ( metadata != null ) {
			OgmEntityPersister persister = (OgmEntityPersister) session.getFactory().getEntityPersister( metadata.getTypeName() );
			return persister.getTupleContext( session );
		}
		return null;
	}
}
