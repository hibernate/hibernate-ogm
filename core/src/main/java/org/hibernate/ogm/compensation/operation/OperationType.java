/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation;

/**
 * The type of a {@link GridDialectOperation}.
 *
 * @author Gunnar Morling
 */
public enum OperationType {

	// GridDialect
	CREATE_TUPLE_WITH_KEY,
	INSERT_OR_UPDATE_TUPLE,
	REMOVE_TUPLE,
	CREATE_ASSOCIATION_WITH_KEY,
	INSERT_OR_UPDATE_ASSOCIATION,
	REMOVE_ASSOCIATION,
	EXECUTE_BATCH,
	FLUSH_PENDING_OPERATIONS,

	// IdentityColumnAwareGridDialect

	CREATE_TUPLE,
	INSERT_TUPLE,

	// OptimisticLockingAwareGridDialect
	UPDATE_TUPLE_WITH_OPTIMISTIC_LOCK,
	REMOVE_TUPLE_WITH_OPTIMISTIC_LOCK;
}
