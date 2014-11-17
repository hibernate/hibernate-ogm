/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.HibernateException;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Raised by {@link GridDialect} implementations using {@link DuplicateInsertPreventionStrategy#NATIVE} upon insertion
 * of an already existing tuple.
 *
 * @author Gunnar Morling
 */
public class TupleAlreadyExistsException extends HibernateException {

	private final EntityKeyMetadata entityKeyMetadata;
	private final Tuple id;

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKeyMetadata Key metadata for the affected entity
	 * @param id A {@link Tuple} containing the id column(s) of the affected entity
	 * @param cause An exception raised by the underlying datastore indicating the insertion of a duplicate primary key
	 */
	public TupleAlreadyExistsException(EntityKeyMetadata entityKeyMetadata, Tuple id, Throwable cause) {
		super( cause );

		this.entityKeyMetadata = entityKeyMetadata;
		this.id = id;
	}

	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	public Tuple getId() {
		return id;
	}
}
