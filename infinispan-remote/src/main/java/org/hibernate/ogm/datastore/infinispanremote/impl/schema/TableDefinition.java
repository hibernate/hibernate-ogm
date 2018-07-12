/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.schema;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.mapping.Column;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.CompositeProtobufCoDec;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.ProtofieldAccessorSet;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.schema.SchemaDefinitions;
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
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;

public final class TableDefinition implements ProtobufTypeExporter, ProtobufEntryExporter {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final String tableName;
	private final String protobufTypeName;
	private final String protobufIdTypeName;
	private final String protobufPackageName;
	private final String cacheConfiguration;
	private final ProtofieldAccessorSet keyComponents = new ProtofieldAccessorSet();
	private final ProtofieldAccessorSet valueComponents = new ProtofieldAccessorSet();
	private final Set<String> pkColumnNames = new HashSet<>();

	public TableDefinition(String name, String protobufPackageName, String cacheConfiguration) {
		this.tableName = name;
		this.protobufTypeName = SanitationUtils.convertNameSafely( name );
		this.protobufIdTypeName = SanitationUtils.toProtobufIdName( protobufTypeName );
		this.protobufPackageName = protobufPackageName;
		this.cacheConfiguration = cacheConfiguration;
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
			CompositeProtobufCoDec codec = new CompositeProtobufCoDec(
					qualify( protobufTypeName ), qualify( protobufIdTypeName ),
					keyComponents, valueComponents, remoteCache, sd );
			ProtostreamSerializerSetup.registerEntityMarshaller( codec, marshaller );
			return new ProtoDataMapper( codec );
		}
		catch (DescriptorParserException e) {
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

	public String getCacheConfiguration() {
		return cacheConfiguration;
	}

	public boolean isDescribedIn(FileDescriptor fileDescriptor) {
		boolean typeIsDescribed = false;
		boolean idTypeIsDescribed = false;

		for ( Descriptor descriptor : fileDescriptor.getMessageTypes() ) {
			if ( descriptor.getName().equals( protobufIdTypeName ) ) {
				if ( idTypeIsDescribedIn( descriptor ) ) {
					idTypeIsDescribed = true;
				}
			}
			if ( descriptor.getName().equals( protobufTypeName ) ) {
				if ( typeIsDescribedIn( descriptor ) ) {
					typeIsDescribed = true;
				}
			}
		}

		// both key and value types must be described
		return typeIsDescribed && idTypeIsDescribed;
	}

	private boolean idTypeIsDescribedIn(Descriptor descriptor) {
		return keyComponents.isDescribedIn( descriptor );
	}

	private boolean typeIsDescribedIn(Descriptor descriptor) {
		return valueComponents.isDescribedIn( descriptor );
	}
}
