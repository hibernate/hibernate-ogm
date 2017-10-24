/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.schema.impl;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;

/**
 * A {@link SchemaManagementTool} for NoSql datastores in OGM.
 *
 * @author Gunnar Morling
 */

/*
 * Note: For now we ignore given {@link Target}s due to the way they are used in ORM's {@link SchemaDefiner}: Instead of
 * passing the actual targets (for denoting stout/file/database), a helper target is used for collecting all SQL
 * commands there. As long as that's the case we cannot really derive the intended export target from that. {@code
 * SchemaExport} is in a transitional state as per Steve, so we should be able to do the right thing down the road. For
 * now, only DB export will be supported, but no file exports (if feasible with given stores anyways, e.g. for MongoDB
 * it will be hard to come up with sensible file exports; We could create JavaScript commands representing "DDL", but
 * it'd be tough to re-import and execute them later on).
 */
public class NoSqlSchemaManagementTool implements SchemaManagementTool {

	private final SessionFactoryImplementor factory;

	public NoSqlSchemaManagementTool(SessionFactoryImplementor sessionFactory) {
		this.factory = sessionFactory;
	}

	@Override
	public SchemaCreator getSchemaCreator(Map options) {
		return new NoSqlSchemaCreator( factory );
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new NoSqlSchemaDropper( factory );
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		throw new UnsupportedOperationException( "Schema migration is not supported yet" );
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		return new NoSqlSchemaValidator( factory );
	}
}
