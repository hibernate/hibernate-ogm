/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception;

import org.hibernate.HibernateException;

/**
 * Indicates that the insertion of an entity failed as another entity with the same id already exists in the datastore.
 *
 * @author Gunnar Morling
 */
public class EntityAlreadyExistsException extends HibernateException {

	public EntityAlreadyExistsException(String message) {
		super( message );
	}

	public EntityAlreadyExistsException(Throwable cause) {
		super( cause );
	}

	public EntityAlreadyExistsException(String message, Throwable cause) {
		super( message, cause );
	}
}
