/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.orientdb.OrientDBDialect;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.DatabaseTypeEnum;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.StorageModeEnum;
import org.hibernate.ogm.datastore.orientdb.connection.DatabaseHolder;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdb.schema.OrientDBDocumentSchemaDefiner;
import org.hibernate.ogm.datastore.orientdb.transaction.impl.OrientDbTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.orientdb.utils.FormatterUtil;
import org.hibernate.ogm.datastore.orientdb.utils.PropertyReaderUtil;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final long serialVersionUID = 1L;
	private static Log log = LoggerFactory.getLogger();
	private static final Set<StorageModeEnum> SUPPORTED_STORAGES = EnumSet.of( StorageModeEnum.MEMORY, StorageModeEnum.PLOCAL );
	private DatabaseHolder databaseHolder;
	private ConfigurationPropertyReader propertyReader;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return OrientDBDialect.class;
	}

	@Override
	public void start() {
		log.debug( "---start---" );
		try {
			StorageModeEnum storageMode = PropertyReaderUtil.readStorateModeProperty( propertyReader, getDefaultStorage(), getSupportedStorages() );
			DatabaseTypeEnum databaseType = PropertyReaderUtil.readDatabaseTypeProperty( propertyReader );

			if ( DatabaseTypeEnum.GRAPH.equals( databaseType ) ) {
				throw new UnsupportedOperationException( "Graph API is not supported yet. Use Document API!" );
			}

			String user = PropertyReaderUtil.readUserProperty( propertyReader );
			String password = PropertyReaderUtil.readPasswordProperty( propertyReader );
			Integer poolSize = PropertyReaderUtil.readPoolSizeProperty( propertyReader );
			String orientDBUrl = prepareOrientDbUrl( storageMode );
			log.debugf( "connect to URL %s", orientDBUrl );

			if ( PropertyReaderUtil.readCreateDatabaseProperty( propertyReader ) ) {
				createDB( orientDBUrl, storageMode, databaseType, poolSize );
			}

			databaseHolder = new DatabaseHolder( orientDBUrl, user, password, poolSize );

			FormatterUtil.setDateFormatter( createFormatter( propertyReader, OrientDBProperties.DATE_FORMAT, OrientDBConstant.DEFAULT_DATE_FORMAT ) );
			FormatterUtil
					.setDateTimeFormatter( createFormatter( propertyReader, OrientDBProperties.DATETIME_FORMAT, OrientDBConstant.DEFAULT_DATETIME_FORMAT ) );
		}
		catch (Exception e) {
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	private ThreadLocal<DateFormat> createFormatter(final ConfigurationPropertyReader propertyReader, final String property, final String defaultFormat) {
		return new ThreadLocal<DateFormat>() {

			@Override
			protected DateFormat initialValue() {
				return new SimpleDateFormat( propertyReader.property( property, String.class ).withDefault( defaultFormat ).getValue() );
			}
		};
	}

	private String prepareOrientDbUrl(StorageModeEnum storage) {
		String database = PropertyReaderUtil.readDatabaseProperty( propertyReader );
		StringBuilder orientDbUrl = new StringBuilder( 100 );
		orientDbUrl.append( storage.name().toLowerCase() );
		orientDbUrl.append( ":" );

		switch ( storage ) {
			case MEMORY:
				orientDbUrl.append( database );
				break;
			case PLOCAL:
				String path = PropertyReaderUtil.readDatabasePathProperty( propertyReader );
				orientDbUrl.append( path ).append( "/" ).append( database );
				break;
			case REMOTE:
				String host = PropertyReaderUtil.readHostProperty( propertyReader );
				orientDbUrl.append( host ).append( "/" ).append( database );
				break;
			default:
				throw log.unsupportedStorage( storage );
		}
		return orientDbUrl.toString();
	}

	protected void createDB(String orientDbUrl, StorageModeEnum storage, DatabaseTypeEnum databaseType, Integer poolSize) {
		log.debug( "---createDB---" );
		String user = PropertyReaderUtil.readUserProperty( propertyReader );
		String password = PropertyReaderUtil.readPasswordProperty( propertyReader );
		log.debugf( "User: %s; Password: %s ", user, password );
		if ( StorageModeEnum.MEMORY.equals( storage ) ||
				StorageModeEnum.PLOCAL.equals( storage ) ) {
			try {
				OPartitionedDatabasePoolFactory factory = new OPartitionedDatabasePoolFactory( poolSize );
				OPartitionedDatabasePool pool = factory.get( orientDbUrl, user, password );
				pool.setAutoCreate( true );
				ODatabaseDocumentTx db = pool.acquire();
				log.debugf( "db.isClosed(): %b", db.isClosed() );
				log.debugf( "db.isActiveOnCurrentThread(): %b", db.isActiveOnCurrentThread() );
			}
			catch (Exception e) {
				throw log.cannotCreateDatabase( String.format( "Can not create OrientDB URL %s", orientDbUrl ), e );
			}
		}
		else {
			throw log.unsupportedStorage( storage );
		}
	}

	public ODatabaseDocumentTx getCurrentDatabase() {
		return databaseHolder.get();
	}

	protected StorageModeEnum getDefaultStorage() {
		return StorageModeEnum.MEMORY;
	}

	protected Set<StorageModeEnum> getSupportedStorages() {
		return SUPPORTED_STORAGES;
	}

	public void closeCurrentDatabase() {
		databaseHolder.remove();
	}

	@Override
	public void stop() {
		log.debug( "---stop---" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void configure(Map cfg) {
		log.debugf( "config map: %s", cfg.toString() );
		propertyReader = new ConfigurationPropertyReader( cfg );
	}

	public ConfigurationPropertyReader getPropertyReader() {
		return propertyReader;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return OrientDBDocumentSchemaDefiner.class;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new OrientDbTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
