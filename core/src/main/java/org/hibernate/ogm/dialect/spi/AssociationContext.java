/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public interface AssociationContext {

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	OperationsQueue getOperationsQueue();

	/**
	 * Provides context information related to the given association's type.
	 * @return Context information related to the given association's type
	 */
	AssociationTypeContext getAssociationTypeContext();

	/**
	 * Returns a tuple representing the entity on the current side of the association for which the given operation was
	 * invoked. May be {@code null} in case this context is passed during the deletion of a transient entity.
	 *
	 * @return A tuple representing the entity on the current side of the association
	 */
	Tuple getEntityTuple();
}
