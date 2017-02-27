/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.HibernateException;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * Raised by {@link GridDialect} implementations using {@link DuplicateInsertPreventionStrategy#NATIVE} upon insertion
 * of an already existing tuple.
 *
 * @author Gunnar Morling
 */
public class TupleAlreadyExistsException extends HibernateException {

	/**
	 * The {@link EntityKeyMetadata} of the tuple.
	 */
	private final EntityKeyMetadata entityKeyMetadata;

	/**
	 * The {@link EntityKey} of the tuple.
	 *
	 * Might be null if the exception is thrown during a batched operation.
	 */
	private final EntityKey entityKey;

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKey An {@link EntityKey} containing the id of the affected entity
	 */
	public TupleAlreadyExistsException(EntityKey entityKey) {
		super( (Throwable) null );

		this.entityKey = entityKey;
		this.entityKeyMetadata = entityKey.getMetadata();
	}

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKey An {@link EntityKey} containing the id of the affected entity
	 * @param message a message explaining the cause of the error
	 */
	public TupleAlreadyExistsException(EntityKey entityKey, String message) {
		super( message );

		this.entityKey = entityKey;
		this.entityKeyMetadata = entityKey.getMetadata();
	}

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKey An {@link EntityKey} containing the id of the affected entity
	 * @param cause An exception raised by the underlying datastore indicating the insertion of a duplicate primary key
	 */
	public TupleAlreadyExistsException(EntityKey entityKey, Throwable cause) {
		super( cause );

		this.entityKey = entityKey;
		this.entityKeyMetadata = entityKey.getMetadata();
	}

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKeyMetadata Key metadata for the affected entity
	 */
	public TupleAlreadyExistsException(EntityKeyMetadata entityKeyMetadata) {
		super( (Throwable) null );

		this.entityKey = null;
		this.entityKeyMetadata = entityKey.getMetadata();
	}

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKeyMetadata Key metadata for the affected entity
	 * @param message a message explaining the cause of the error
	 */
	public TupleAlreadyExistsException(EntityKeyMetadata entityKeyMetadata, String message) {
		super( message );

		this.entityKey = null;
		this.entityKeyMetadata = entityKey.getMetadata();
	}

	/**
	 * Creates a new {@code TupleAlreadyExistsException}.
	 *
	 * @param entityKeyMetadata Key metadata for the affected entity
	 * @param cause An exception raised by the underlying datastore indicating the insertion of a duplicate primary key
	 */
	public TupleAlreadyExistsException(EntityKeyMetadata entityKeyMetadata, Throwable cause) {
		super( cause );

		this.entityKey = null;
		this.entityKeyMetadata = entityKeyMetadata;
	}

	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}
}
