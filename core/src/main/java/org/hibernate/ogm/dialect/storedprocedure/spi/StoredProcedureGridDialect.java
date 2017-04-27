/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.storedprocedure.spi;

import java.io.Serializable;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A facet for {@link GridDialect} implementations which support the execution of stored procedures.
 *
 * @param <T> The type of native queries supported by this dialect
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public interface StoredProcedureGridDialect<T extends Serializable> extends GridDialect {
	/**
	 * Returns the result of a stored procedure executed on the backend.
	 *
	 * @param storedProcedureName name of stored procedure.
	 * @param tupleContext the tuple context
	 *
	 * @return an {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> callStoredProcedure(String storedProcedureName, TupleContext tupleContext);

}
