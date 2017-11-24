/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.schema;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.mapping.Column;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.CompositeProtobufCoDec;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.ProtofieldAccessorSet;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.TypeDeclarationsCollector;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtoDataMapper;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamSerializerSetup;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.SerializationContext;

public final class TableDefinition implements ProtobufTypeExporter, ProtobufEntryExporter {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final String tableName;
	private final String protobufTypeName;
	private final String protobufIdTypeName;
	private final String protobufPackageName;
	private final String cacheTemplate;
	private final ProtofieldAccessorSet keyComponents = new ProtofieldAccessorSet();
	private final ProtofieldAccessorSet valueComponents = new ProtofieldAccessorSet();
	private final Set<String> pkColumnNames = new HashSet<>();

	public TableDefinition(String name, String protobufPackageName, String cacheTemplate) {
		this.tableName = name;
		this.protobufTypeName = SanitationUtils.convertNameSafely( name );
		this.protobufIdTypeName = SanitationUtils.toProtobufIdName( protobufTypeName );
		this.protobufPackageName = protobufPackageName;
		this.cacheTemplate = cacheTemplate;
	}

	public void addColumnnDefinition(Column column, GridType gridType, Type ormType) {
		//final boolean nullable = column.isNullable(); //Seems not reliable for FKs, don't use this to determine nullability.
		final String name = column.getName();
		if ( pkColumnNames.contains( name ) ) {
			keyComponents.addMapping( name, gridType, ormType, false );
			valueComponents.addMapping( name, gridType, ormType, false );
		}
		else {
			valueComponents.addMapping( name, gridType, ormType, true );
		}
	}

	@Override
	public void exportProtobufEntry(StringBuilder output) {
		exportProtobufEntry( protobufIdTypeName, keyComponents, output );
		exportProtobufEntry( protobufTypeName, valueComponents, output );
	}

	private void exportProtobufEntry(String typeName, ProtofieldAccessorSet fields, StringBuilder sb) {
		sb.append( "\nmessage " ).append( typeName ).append( " {" );
		fields.forEachProtobufFieldExporter( v -> v.exportProtobufFieldDefinition( sb ) );
		sb.append( "\n}\n" );
	}

	@Override
	public void collectTypeDefinitions(TypeDeclarationsCollector typesDefCollector) {
		keyComponents.forEach( v -> v.collectTypeDefinitions( typesDefCollector ) );
		valueComponents.forEach( v -> v.collectTypeDefinitions( typesDefCollector ) );
	}

	public ProtoDataMapper createProtoDataMapper(RemoteCache remoteCache,
			SchemaDefinitions sd, OgmProtoStreamMarshaller marshaller) {
		try {
			CompositeProtobufCoDec codec = new CompositeProtobufCoDec( tableName,
					qualify( protobufTypeName ), qualify( protobufIdTypeName ),
					keyComponents, valueComponents, remoteCache, sd );
			SerializationContext serializationContext = ProtostreamSerializerSetup.buildSerializationContext( sd, codec );
			return new ProtoDataMapper( codec, serializationContext, marshaller );
		}
		catch (DescriptorParserException | IOException e) {
			throw new RuntimeException( e );
		}
	}

	private String qualify(final String name) {
		return SanitationUtils.qualify( name, protobufPackageName );
	}

	public String getTableName() {
		return tableName;
	}

	public void markAsPrimaryKey(String name) {
		pkColumnNames.add( name );
	}

	public void validate() {
		//This is triggered by certain Bag collection types:
		//we can't support mapping w/o a primary key
		//as they can't be mapped on a K/V system.
		//The user can avoid the error by picking some ordering strategy.
		if ( pkColumnNames.isEmpty() ) {
			throw log.tableHasNoPrimaryKey( tableName );
		}
	}

	public String getCacheTemplate() {
		return cacheTemplate;
	}
}
