/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation;

/**
 * A strategy for dealing with errors occurring during execution of a grid dialect operation.
 *
 * @author Gunnar Morling
 */
public enum ErrorHandlingStrategy {

	/**
	 * The current unit of work will be aborted, no further grid dialect operations will be executed. The causing
	 * exception will be raised.
	 */
	ABORT,

	/**
	 * The current unit of work will be continued, the remaining grid dialect operations will be executed. The causing
	 * exception will be ignored.
	 * <p>
	 * Care must be taken with this strategy on transactional datastores: If the exception was raised by the datastore
	 * itself, it may be possible that datastore marks the transaction for rollback, not allowing to commit it later on,
	 * also if the exception has been suppressed.
	 */
	CONTINUE;
}
