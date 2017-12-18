/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.storedprocedure;

import java.util.List;
import java.util.Map;

/**
 * Named or positional parameters to pass to a stored procedure.
 *
 * @author Davide D'Alto
 */
public class ProcedureQueryParameters {

	private final Map<String, Object> namedParameters;
	private final List<Object> positionalParameters;

	/**
	 * Create a new instance containing the values of the parameters to pass to the stored procedure.
	 *
	 * @param namedParameters a map where the key is the parameter name and value the parameter value
	 * @param positionalParameters a list of parameters in the right order to be passed
	 */
	public ProcedureQueryParameters(Map<String, Object> namedParameters, List<Object> positionalParameters) {
		this.namedParameters = namedParameters;
		this.positionalParameters = positionalParameters;
	}

	public List<Object> getPositionalParameters() {
		return positionalParameters;
	}

	public Map<String, Object> getNamedParameters() {
		return namedParameters;
	}
}
