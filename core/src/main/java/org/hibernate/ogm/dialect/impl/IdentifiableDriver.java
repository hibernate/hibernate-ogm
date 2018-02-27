/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * A {@link TransactionDriver} that can return an identifier for the underlying transaction.
 *
 * @author Davide D'Alto
 */
public interface IdentifiableDriver extends TransactionDriver {

	Object getTransactionId();
}
