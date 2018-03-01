/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.storedprocedure.spi;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.storedprocedure.ProcedureQueryParameters;

/**
 * A facet for {@link GridDialect} implementations which support the execution of stored procedures.
 * <p>
 * Cases of stored procedures are:
 * <ol>
 * <li>procedure without any input or output parameters</li>
 * <li>function with many input parameters and one returned value (primitive)</li>
 * <li>function with many input parameters and result set</li>
 * </ol>
 *
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public interface StoredProcedureAwareGridDialect extends GridDialect {

	/**
	 * Returns the result of a stored procedure executed on the backend.
	 *
	 * @param storedProcedureName name of stored procedure
	 * @param queryParameters parameters passed for this query
	 * @param tupleContext the tuple context
	 *
	 * @return a {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> callStoredProcedure( String storedProcedureName, ProcedureQueryParameters queryParameters, TupleContext tupleContext);
}
