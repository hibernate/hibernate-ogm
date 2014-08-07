/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class AssociationContext implements GridDialectOperationContext {

	private final OptionsContext optionsContext;
	private final EntityKeyMetadata targetEntityKeyMetadata;
	private final AssociationKeyMetadata targetAssociationKeyMetada;
	private OperationsQueue operationsQueue;

	public AssociationContext(OptionsContext optionsContext, EntityKeyMetadata targetEntityKeyMetadata, AssociationKeyMetadata targetAssociationKeyMetada) {
		this.optionsContext = optionsContext;
		this.targetEntityKeyMetadata = targetEntityKeyMetadata;
		this.targetAssociationKeyMetada = targetAssociationKeyMetada;
	}

	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	public void setOperationsQueue(OperationsQueue operationsQueue) {
		this.operationsQueue = operationsQueue;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	/**
	 * Return the metadata of the entity that is the target of the association.
	 * Note that for embedded collection this value is null and this information is stored in the RowKey.
	 */
	public EntityKeyMetadata getTargetEntityKeyMetadata() {
		return targetEntityKeyMetadata;
	}

	/**
	 * The column identifying the target side of the association
	 */
	public AssociationKeyMetadata getTargetAssociationKeyMetadata() {
		return targetAssociationKeyMetada;
	}

	@Override
	public String toString() {
		return "AssociationContext [optionsContext=" + optionsContext + "]";
	}
}
