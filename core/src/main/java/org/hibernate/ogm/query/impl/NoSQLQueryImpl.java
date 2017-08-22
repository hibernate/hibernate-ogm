/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.query.NoSQLQueryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.internal.NativeQueryImpl;

/**
 * Hibernate OGM implementation of the {@link NativeQuery} contract.
 * <p>
 * This class is mainly here for historical reasons. We keep it anyway as it might be useful in the future to have our
 * own specialized {@code NativeQuery}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Smet
 */
public class NoSQLQueryImpl<R> extends NativeQueryImpl<R> implements NoSQLQueryImplementor<R> {

	/**
	 * Constructs a NoSQLQuery given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined query.
	 * @param session The session to which this NoSQLQuery belongs.
	 * @param parameterMetadata Metadata about parameters found in the query.
	 */
	public NoSQLQueryImpl(NamedSQLQueryDefinition queryDef, SharedSessionContractImplementor session, ParameterMetadata parameterMetadata) {
		super( queryDef, session, parameterMetadata );
	}

	public NoSQLQueryImpl(String sql, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( sql, false, session, parameterMetadata );
	}

	public NoSQLQueryImpl(String sql, boolean callable, SharedSessionContractImplementor session, ParameterMetadata parameterMetadata) {
		super( sql, callable, session, parameterMetadata );
	}
}
