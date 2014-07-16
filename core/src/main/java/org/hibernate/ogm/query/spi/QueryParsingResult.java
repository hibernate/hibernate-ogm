/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.spi;

import java.util.List;

import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.ogm.util.Experimental;

/**
 * Represents the result of parsing a JP-QL query by a {@link QueryParserService} implementation. Subsequentially, this
 * will contain all the information required by {@link QueryTranslator}s which is currently obtained from the existing
 * query parser.
 *
 * @author Gunnar Morling
 */
@Experimental("This contract is under active development.")
public interface QueryParsingResult {

	/**
	 * The resulting query in a representation understood by the underlying datastore, e.g. it may be based on
	 * {@code DBObject}s in case of MongoDB or it may be a String representing a Cypher query in case of Neo4j.
	 *
	 * @return The resulting query
	 */
	Object getQueryObject();

	/**
	 * The names of selected scalar columns.
	 * @return the names of selected scalar columns
	 */
	// TODO Only used for pure projection atm. To be replaced with something which works for mixing returns of different
	// kinds such as scalars, entities or constructor returns
	List<String> getColumnNames();
}
