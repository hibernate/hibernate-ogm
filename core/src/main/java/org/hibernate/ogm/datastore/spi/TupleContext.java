/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.List;

import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class TupleContext implements GridDialectOperationContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;
	private final OperationsQueue operationsQueue;

	public TupleContext(List<String> selectableColumns, OptionsContext optionsContext) {
		this.selectableColumns = selectableColumns;
		this.optionsContext = optionsContext;
		this.operationsQueue = null;
	}

	public TupleContext(List<String> selectableColumns, OptionsContext optionsContext, OperationsQueue operationsQueue) {
		this.selectableColumns = selectableColumns;
		this.optionsContext = optionsContext;
		this.operationsQueue = operationsQueue;
	}

	/**
	 * Returns the mapped columns of the given entity. May be used by a dialect to only load those columns instead of
	 * the complete document/record.
	 */
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Tuple Context {" );

		builder.append( StringHelper.join( selectableColumns, ", " ) );
		builder.append( "}" );

		return builder.toString();
	}
}
