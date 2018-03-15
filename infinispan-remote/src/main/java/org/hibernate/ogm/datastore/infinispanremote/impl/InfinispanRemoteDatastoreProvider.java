/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteDialect;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtoDataMapper;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamSerializerSetup;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.HotRodSequenceHandler;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.EffectivelyFinal;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * @author Sanne Grinovero
 */
public class InfinispanRemoteDatastoreProvider extends BaseDatastoreProvider
				implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * The org.infinispan.commons.marshall.Marshaller instance which shall be used
	 * by our Hot Rod client.
	 */
	private final OgmProtoStreamMarshaller marshaller = new OgmProtoStreamMarshaller();

	// Only available during configuration
	private InfinispanRemoteConfiguration config;

	private boolean createCachesEnabled = false;

	@EffectivelyFinal
	private String newCacheTemplate;

	// The Hot Rod client; maintains TCP connections to the datagrid.
	@EffectivelyFinal
	private RemoteCacheManager hotrodClient;

	//Useful to allow people to dump the generated schema,
	//we use it to capture the schema in tests too.
	@EffectivelyFinal
	private SchemaCapture schemaCapture;

	@EffectivelyFinal
	private ServiceRegistryImplementor serviceRegistry;

	@EffectivelyFinal
	private SchemaOverride schemaOverrideService;

	//For each cache we have a schema and a set of encoders/decoders to the generated protobuf schema
	@EffectivelyFinal
	private Map<String,ProtoDataMapper> perCacheSchemaMappers;

	@EffectivelyFinal
	private HotRodSequenceHandler sequences;

	@EffectivelyFinal
	private HotRodCacheCreationHandler cacheCreation;

	@EffectivelyFinal
	private SchemaDefinitions sd;

	@EffectivelyFinal
	private String schemaPackageName;

	@EffectivelyFinal
	private String schemaFileName;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanRemoteDialect.class;
	}

	@Override
	public void start() {
		hotrodClient = HotRodClientBuilder.builder().withConfiguration( config, marshaller ).build();
		hotrodClient.start();
		config = null; //no longer needed
	}

	public RemoteCacheManager getRemoteCacheManager() {
		return hotrodClient;
	}

	@Override
	public void stop() {
		hotrodClient.stop();
	}

	@Override
	public void configure(Map configurationValues) {
		this.config = new InfinispanRemoteConfiguration();
		this.config.initConfiguration( configurationValues, serviceRegistry );
		this.schemaCapture = config.getSchemaCaptureService();
		this.schemaOverrideService = config.getSchemaOverrideService();
		this.schemaPackageName = config.getSchemaPackageName();
		this.schemaFileName = config.getSchemaFileName();
		this.createCachesEnabled = config.isCreateCachesEnabled();
		this.newCacheTemplate = config.getNewCacheTemplate();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return ProtobufSchemaInitializer.class;
	}

	public void registerSchemaDefinitions(SchemaDefinitions sd) {
		this.sd = sd;
		sd.validateSchema();
		RemoteCache<String,String> protobufCache = getProtobufCache();
		sd.deploySchema( schemaFileName, protobufCache, schemaCapture, schemaOverrideService );

		// register proto schema also to global serialization context used for unmarshalling
		try {
			marshaller.getSerializationContext().registerProtoFiles( sd.asFileDescriptorSource() );
		}
		catch (DescriptorParserException | IOException e) {
			throw log.errorAtProtobufParsing( e );
		}

		this.sequences = new HotRodSequenceHandler( this, marshaller, sd.getSequenceDefinitions() );

		cacheCreation = new HotRodCacheCreationHandler( createCachesEnabled, newCacheTemplate, sd.getCacheTemplateByName() );
		cacheCreation.startAndValidateCaches( hotrodClient );

		perCacheSchemaMappers = sd.generateSchemaMappingAdapters( this, sd, marshaller );
	}

	private RemoteCache<String, String> getProtobufCache() {
		return getCache( ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME );
	}

	@Override
	public boolean allowsTransactionEmulation() {
		// Hot Rod doesn't support "true" transaction yet
		return true;
	}

	public String getProtobufPackageName() {
		return schemaPackageName;
	}

	public String getTemplate(String cacheName) {
		return cacheCreation.getTemplate( cacheName );
	}

	public Set<String> getMappedCacheNames() {
		return cacheCreation.getMappedCacheNames();
	}

	public ProtoStreamMappingAdapter getDataMapperForCache(String cacheName) {
		return perCacheSchemaMappers.get( cacheName );
	}

	public ProtostreamAssociationMappingAdapter getCollectionsDataMapper(String cacheName) {
		return perCacheSchemaMappers.get( cacheName );
	}

	public HotRodSequenceHandler getSequenceHandler() {
		return this.sequences;
	}

	public SerializationContext getSerializationContextForSequences(SequenceTableDefinition std) {
		//This method is here so that we can cache / reuse these contexts ?
		return ProtostreamSerializerSetup.buildSerializationContextForSequences( sd, std );
	}

	public <K, V> RemoteCache<K, V> getCache(String cacheName) {
		RemoteCache<K,V> cache = hotrodClient.getCache( cacheName );
		if ( cache == null ) {
			throw log.expectedCacheNotDefined( cacheName );
		}
		return cache;
	}


}
