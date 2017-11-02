/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.storedprocedure.spi;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A facet for {@link GridDialect} implementations which support the execution of stored procedures.
 *
 * Cases of stored procedures are :
 * <ol>
 *     <li>procedure without any input or output parameters</li>
 *	   <li>function with many input parameters and one returned value</li>
 *	   <li>function with many input parameters and result set</li>
 * </ol>
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public interface StoredProcedureAwareGridDialect extends GridDialect {

	/**
	 * Is data storage supports parameter position by name (true) or by index (false)
	 *
	 * @return
	 */
	boolean supportsNamedParameters();


	/**
	 * Returns the result of a stored procedure executed on the backend.
	 * Tne method uses for storages that supports name position
	 *
	 * @param storedProcedureName name of stored procedure.
	 * @param queryParameters parameters passed for this query
	 * @param tupleContext the tuple context
	 *
	 * @return an {@link ClosableIterator} with the result of the query
	 */

	ClosableIterator<Tuple> callStoredProcedure( String storedProcedureName, QueryParameters queryParameters, TupleContext tupleContext);
}
