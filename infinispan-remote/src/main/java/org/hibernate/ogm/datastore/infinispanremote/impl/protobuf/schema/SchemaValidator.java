/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.schema;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.TableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.util.impl.ResourceHelper;

import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.parser.SquareProtoParser;

/**
 * Validate a user defined schema using ProtoStream library
 *
 * @author Fabio Massimo Ercoli
 */
public class SchemaValidator {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final SchemaDefinitions owner;
	private final String schemaName;
	private final String protoSchema;

	public SchemaValidator(SchemaDefinitions owner, SchemaOverride schemaOverrideService, URL schemaOverrideResource, String schemaName) {
		this.owner = owner;
		this.schemaName = schemaName;

		if ( schemaOverrideService != null ) {
			protoSchema = schemaOverrideService.createProtobufSchema();
		}
		else if ( schemaOverrideResource != null ) {
			protoSchema = readProtoSchema( schemaOverrideResource );
		}
		else {
			throw new HibernateException( "SchemaValidator#init needs at least one of schemaOverrideService or schemaOverrideResource" );
		}

		validate();
	}

	private String readProtoSchema(URL schemaOverrideResource) {
		try {
			return ResourceHelper.readResource( schemaOverrideResource );
		}
		catch (IOException e) {
			throw LOG.errorLoadingSchemaOverrideResourceFile( schemaOverrideResource );
		}
	}

	public String provideSchema() {
		return protoSchema;
	}

	private void validate() {
		FileDescriptor fileDescriptor;
		try {
			fileDescriptor = parseSchema();
		}
		catch (DescriptorParserException descriptorParserException) {
			throw LOG.providedSchemaHasAnIllegalFormat( descriptorParserException.getMessage(), protoSchema );
		}

		if ( !owner.packageName.equals( fileDescriptor.getPackage() ) ) {
			throw LOG.providedSchemaHasAnInvalidPackageName( owner.packageName, fileDescriptor.getPackage() );
		}
		for ( SequenceTableDefinition tableDefinition : owner.idSchemaPerMetadata.values() ) {
			if ( !tableDefinition.isDescribedIn( fileDescriptor ) ) {
				throw LOG.providedSchemaHasAnInvalidCacheDefinition( tableDefinition.getName() );
			}
		}
		for ( TableDefinition tableDefinition : owner.definitionsByTableName.values() ) {
			if ( !tableDefinition.isDescribedIn( fileDescriptor ) ) {
				throw LOG.providedSchemaHasAnInvalidCacheDefinition( tableDefinition.getTableName() );
			}
		}
	}

	private FileDescriptor parseSchema() {
		FileDescriptorSource fileDescriptorSource = FileDescriptorSource.fromString( schemaName, protoSchema );
		Configuration config = Configuration.builder().build();

		SquareProtoParser protoParser = new SquareProtoParser( config );
		Map<String, FileDescriptor> fileDescriptorMap = protoParser.parse( fileDescriptorSource );
		return fileDescriptorMap.get( schemaName );
	}
}
