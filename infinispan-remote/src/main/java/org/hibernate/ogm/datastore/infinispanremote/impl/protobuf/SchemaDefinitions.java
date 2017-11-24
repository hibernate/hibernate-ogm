/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtoDataMapper;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.TableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.protostream.FileDescriptorSource;

public class SchemaDefinitions {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final String packageName;
	private final Map<String,TableDefinition> definitionsByTableName = new HashMap<>();
	private final Map<IdSourceKeyMetadata, SequenceTableDefinition> idSchemaPerMetadata = new HashMap<>();
	private final Map<String, SequenceTableDefinition> idSchemaPerName = new HashMap<>();

	//guarded by synchronization on this
	private String cachedSchema = null;

	public SchemaDefinitions(String packageName) {
		this.packageName = packageName;
	}

	// N.B. all messages to the server need to be wrapped in /org/infinispan/protostream/message-wrapping.proto
	// (both the schema definitions and the key/value pairs)
	// This resource is defined in the Protostream jar.
	// Typically this is transparently handled by using the Protostream codecs but be aware of it when bypassing Protostream.

	public void deploySchema(String generatedProtobufName, RemoteCache<String, String> protobufCache, SchemaCapture schemaCapture, SchemaOverride schemaOverrideService) {
		final String generatedProtoschema = schemaOverrideService == null ? generateProtoschema() : schemaOverrideService.createProtobufSchema();
		try {
			protobufCache.put( generatedProtobufName, generatedProtoschema );
			String errors = protobufCache.get( generatedProtobufName + ".errors" );
			if ( errors != null ) {
				throw LOG.errorAtSchemaDeploy( generatedProtobufName, errors );
			}
			LOG.successfulSchemaDeploy( generatedProtobufName );
		}
		catch (HotRodClientException hrce) {
			throw LOG.errorAtSchemaDeploy( generatedProtobufName, hrce );
		}
		if ( schemaCapture != null ) {
			schemaCapture.put( generatedProtobufName, generatedProtoschema );
		}
	}

	private synchronized String generateProtoschema() {
		if ( cachedSchema != null ) {
			return cachedSchema;
		}
		TypeDeclarationsCollector typesDefCollector = new TypeDeclarationsCollector();
		StringBuilder sb = new StringBuilder( 400 );
		sb.append( "package " ).append( packageName ).append( ";\n" );
		idSchemaPerMetadata.forEach( ( k, v ) -> v.exportProtobufEntry( sb ) );
		definitionsByTableName.forEach( ( k, v ) -> v.collectTypeDefinitions( typesDefCollector ) );
		typesDefCollector.exportProtobufEntries( sb );
		definitionsByTableName.forEach( ( k, v ) -> v.exportProtobufEntry( sb ) );
		String fullSchema = sb.toString();
		LOG.generatedSchema( fullSchema );
		this.cachedSchema = fullSchema;
		return fullSchema;
	}

	public void registerTableDefinition(TableDefinition td) {
		TableDefinition previous = definitionsByTableName.put( td.getTableName(), td );
		if ( previous != null ) {
			throw new AssertionFailure( "There should be no duplicate table definitions" );
		}
	}

	public Set<String> getTableNames() {
		Set<String> unionSet = new HashSet<>();
		unionSet.addAll(  definitionsByTableName.keySet() );
		unionSet.addAll(  idSchemaPerName.keySet() );
		return Collections.unmodifiableSet( unionSet );
	}

	public Map<String, String> getCacheTemplateByName() {
		Map<String, String> map = new HashMap<>();
		definitionsByTableName.values().forEach( definition -> map.put(
				definition.getTableName(),
				definition.getCacheTemplate()
		) );
		idSchemaPerName.keySet().forEach( tableName -> map.put( tableName, null ) );
		return map;
	}

	public Map<String,ProtoDataMapper> generateSchemaMappingAdapters(InfinispanRemoteDatastoreProvider provider,
			SchemaDefinitions sd, OgmProtoStreamMarshaller marshaller) {
		Map<String,ProtoDataMapper> adaptersCollector = new HashMap<>();
		definitionsByTableName.forEach( ( k, v ) ->
			adaptersCollector.put( k, v.createProtoDataMapper( provider.getCache( k ), sd, marshaller ) )
			);
		return Collections.unmodifiableMap( adaptersCollector );
	}

	public FileDescriptorSource asFileDescriptorSource() throws IOException {
		FileDescriptorSource source = new FileDescriptorSource();
		StringReader stringReader = new StringReader( generateProtoschema() );
		source.addProtoFile( "ogm-generated", stringReader );
		return source;
	}

	public void createSequenceSchemaDefinition(IdSourceKeyMetadata idSourceKeyMetadata, String protobufPackageName) {
		SequenceTableDefinition std = new SequenceTableDefinition( idSourceKeyMetadata, protobufPackageName );
		SequenceTableDefinition previous = idSchemaPerMetadata.put( idSourceKeyMetadata, std );
		if ( previous != null ) {
			throw new AssertionFailure( "There should be no duplicate definitions for SequenceTableDefinition instances" );
		}
		previous = idSchemaPerName.put( std.getName(), std );
		if ( previous != null ) {
			throw new AssertionFailure( "There should be no duplicate definitions for SequenceTableDefinition instances" );
		}
	}

	public Map<String, SequenceTableDefinition> getSequenceDefinitions() {
		return Collections.unmodifiableMap( idSchemaPerName );
	}

	public void validateSchema() {
		for ( TableDefinition td : definitionsByTableName.values() ) {
			td.validate();
		}
	}

}
