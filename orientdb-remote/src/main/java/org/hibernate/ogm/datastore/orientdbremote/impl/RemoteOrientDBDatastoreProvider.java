/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdbremote.impl;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.DatabaseTypeEnum;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.StorageModeEnum;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdbremote.utils.PropertyReaderUtil;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class RemoteOrientDBDatastoreProvider extends OrientDBDatastoreProvider {

	private static Log log = LoggerFactory.getLogger();
	private static final Set<StorageModeEnum> SUPPORTED_STORAGES = EnumSet.of( StorageModeEnum.REMOTE );

	@Override
	protected void createDB(String orientDbUrl, StorageModeEnum storageMode, DatabaseTypeEnum databaseType, Integer poolSize) {
		if ( PropertyReaderUtil.readCreateDatabaseProperty( getPropertyReader() ) ) {
			String rootUser = PropertyReaderUtil.readRootUserProperty( getPropertyReader() );
			String rootPassword = PropertyReaderUtil.readRootPasswordProperty( getPropertyReader() );
			log.debugf( "Root user: %s; root password: %s ", rootUser, rootPassword );
			String host = PropertyReaderUtil.readHostProperty( getPropertyReader() );
			String database = PropertyReaderUtil.readDatabaseProperty( getPropertyReader() );
			log.debugf( "Try to create remote database in URL %s ", orientDbUrl );
			OServerAdmin serverAdmin = null;
			try {
				serverAdmin = new OServerAdmin( "remote:" + host );
				serverAdmin.connect( rootUser, rootPassword );
				boolean isDbExists = serverAdmin.existsDatabase( database, OrientDBConstant.PLOCAL_STORAGE_TYPE );
				log.infof( "Database %s esists? %s.", database, String.valueOf( isDbExists ) );
				if ( !isDbExists ) {
					log.infof( "Database %s not exists. Try to create it.", database );
					serverAdmin.createDatabase( database, databaseType.name().toLowerCase(), OrientDBConstant.PLOCAL_STORAGE_TYPE );
				}
				else {
					log.infof( "Database %s already exists", database );
				}

				// open the database
				@SuppressWarnings("resource")
				ODatabaseDocumentTx db = new ODatabaseDocumentTx( "remote:" + host + "/" + database );
				db.open( rootUser, rootPassword );
			}
			catch (Exception ioe) {
				throw log.cannotCreateDatabase( database, ioe );
			}
			finally {
				if ( serverAdmin != null ) {
					serverAdmin.close( true );
				}
			}
		}
	}

	@Override
	protected StorageModeEnum getDefaultStorage() {
		return OrientDBProperties.StorageModeEnum.REMOTE;
	}

	@Override
	protected Set<StorageModeEnum> getSupportedStorages() {
		return SUPPORTED_STORAGES;
	}
}
