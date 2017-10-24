/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.schema.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * A common base class for the operations to execute on a schema.
 *
 * @author Davide D'Alto
 */
abstract class SchemaAction {

	SchemaDefiner createSchemaInitializer(SessionFactoryImplementor factory, Metadata metadata) {
		ServiceRegistryImplementor registry = factory.getServiceRegistry();
		SchemaDefiner schemaInitializer = registry.getService( SchemaDefiner.class );
		return schemaInitializer;
	}

	void validate(SourceDescriptor sourceDescriptor, TargetDescriptor targetDescriptor) {
		if ( sourceDescriptor.getSourceType() != SourceType.METADATA ) {
			throw new UnsupportedOperationException( "Import scripts for schema creation are not supported at this point by Hibernate OGM" );
		}

		if ( targetDescriptor.getTargetTypes().contains( TargetType.SCRIPT ) ) {
			throw new UnsupportedOperationException( "Only schema export to the datastore is supported at this point by Hibernate OGM" );
		}
	}
}
