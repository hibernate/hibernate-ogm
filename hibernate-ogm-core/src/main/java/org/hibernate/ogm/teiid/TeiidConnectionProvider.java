package org.hibernate.ogm.teiid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.dialect.impl.GridDialectFactory;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.jdbc.TeiidDriver;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.TranslatorException;

class TeiidConnectionProvider implements ConnectionProvider {

	private EmbeddedServer teiidServer;

	public TeiidConnectionProvider(final Map configurationValues, final ServiceRegistryImplementor registry) {
		final JtaPlatform jtaPlatform = registry.getService(JtaPlatform.class);
		
		EmbeddedConfiguration ec = new EmbeddedConfiguration() {
			public TransactionManager getTransactionManager() {
				return jtaPlatform.retrieveTransactionManager();
			}
		};
		ec.setUseDisk(true);
				
		this.teiidServer = new EmbeddedServer();
		this.teiidServer.start(ec);		
		
		EmbeddedServer.ConnectionFactoryProvider<GridDialectFactory> gridFactory = new EmbeddedServer.ConnectionFactoryProvider<GridDialectFactory>() {
			@Override
			public GridDialectFactory getConnectionFactory() throws TranslatorException {
				return registry.getService(GridDialectFactory.class);
			}
		};
		
		this.teiidServer.addConnectionFactoryProvider("grid-source", gridFactory);
		this.teiidServer.addTranslator(new GridExecutionFactory());
		try {
			addTempVDB();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	private void addTempVDB() throws TranslatorException, VirtualDatabaseException, ConnectorManagerException {
		ModelMetaData gridModel = new ModelMetaData();
		gridModel.setName("temp");
		gridModel.setSchemaSourceType("ddl");
		gridModel.setSchemaText("CREATE FOREIGN TABLE G1 (e1 string, e2 integer);");
		gridModel.addSourceMapping("grid-connector", "grid-translator", "grid-source");

		// deploy the VDB to the embedded server
		teiidServer.deployVDB("thevdb", gridModel);
	}	
	
	public void addVDB(Iterator<Table> tables, SessionFactoryImpl sf) throws TranslatorException, VirtualDatabaseException, ConnectorManagerException {
		teiidServer.undeployVDB("thevdb");
		this.teiidServer.addMetadataRepository("grid-metadata", new GridMetadataRespository(tables, sf));
		
		ModelMetaData gridModel = new ModelMetaData();
		gridModel.setName("grid");
		gridModel.setSchemaSourceType("grid-metadata");
		gridModel.addSourceMapping("grid-connector", "grid-translator", "grid-source");

		// deploy the VDB to the embedded server
		teiidServer.deployVDB("thevdb", gridModel);
	}

	@Override
	public Connection getConnection() throws SQLException {
		TeiidDriver driver = teiidServer.getDriver();
		return driver.connect("jdbc:teiid:thevdb", null);
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	
	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		throw new UnknownUnwrapTypeException(unwrapType);
	}
}
