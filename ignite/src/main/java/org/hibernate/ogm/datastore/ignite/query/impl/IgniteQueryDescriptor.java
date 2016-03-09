/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.type.Type;

public class IgniteQueryDescriptor implements Serializable {

	private static final long serialVersionUID = 8197979441369153954L;

	private final String originalSql;
	private final String sql;
	private final boolean hasScalar;
	private final List<Return> customQueryReturns;
	private final Set<String> querySpaces;

	public IgniteQueryDescriptor(String originalSql, String resultSql, SelectClause selectClause, Set<String> querySpaces) {
		this.originalSql = originalSql;
		this.sql = resultSql;
		this.hasScalar = selectClause.isScalarSelect();
		List<Return> returnList = new ArrayList<>();
		List<String> columns = new ArrayList<String>();
		if (selectClause.getColumnNames() != null && selectClause.getColumnNames().length > 0) {
			for (int i = 0; i < selectClause.getColumnNames().length; i++) {
				columns.addAll( Arrays.asList( selectClause.getColumnNames()[i] ) );
			}
		}
		String[] aliases = columns.toArray( new String[columns.size()] );
		Type[] types = selectClause.getQueryReturnTypes();
		for (int i = 0; i < selectClause.getQueryReturnTypes().length; i++) {
			returnList.add( new ScalarReturn(types[i], aliases != null ? aliases[i] : null) );
		}
		this.customQueryReturns = Collections.unmodifiableList( returnList );
		this.querySpaces = querySpaces;
	}

	public IgniteQueryDescriptor(String originalSql, String sql) {
		this.originalSql = originalSql;
		this.sql = sql;
		// SQL queries working only for scalars queries
		this.hasScalar = true;
		this.customQueryReturns = null;
		this.querySpaces = Collections.emptySet();
	}

	public String getOriginalSql() {
		return originalSql;
	}

	public String getSql() {
		return sql;
	}

	public boolean isHasScalar() {
		return hasScalar;
	}

	public List<Return> getCustomQueryReturns() {
		return customQueryReturns;
	}

	public Set<String> getQuerySpaces() {
		return querySpaces;
	}

}
