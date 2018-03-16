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

/**
 * A {@link MultiTableBulkIdStrategy} that does notghing.
 * <p>
 * In Hibernate OGM we don't support cases where the entity is split in multiple table because
 * some datastores don't have join operations.
 * <p>
 * This class was introduced because Hibernate ORM sometimes needs an implementation anyway
 * and having the default one would cause exceptions.
 *
 * @author Davide D'Alto
 */
public class NoOpMultiTableBulkIdStrategy implements MultiTableBulkIdStrategy {

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
