/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;

public class EmptyMultiTableBulkIdStrategy implements MultiTableBulkIdStrategy {
	@Override
	public void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			SessionFactoryOptions sessionFactoryOptions) {

	}

	@Override
	public void release(
			JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess) {

	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return null;
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SessionFactoryImplementor factory, HqlSqlWalker walker) {
		return null;
	}
}
