/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.teiid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.dialect.GridTranslator;
import org.hibernate.ogm.dialect.impl.GridDialectFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
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

@SuppressWarnings({"nls"})
public class TeiidConnectionProvider implements ConnectionProvider {

	private EmbeddedServer teiidServer;

	public TeiidConnectionProvider(final Map configurationValues, final ServiceRegistryImplementor registry) {
		final JtaPlatform jtaPlatform = registry.getService(JtaPlatform.class);
		//final GridTranslator translator = registry.getService(GridTranslator.class);
		final GridTranslator translator = new GridExecutionFactory();
		
		EmbeddedConfiguration ec = new EmbeddedConfiguration() {
			public TransactionManager getTransactionManager() {
				return jtaPlatform.retrieveTransactionManager();
			}
		};
		ec.setUseDisk(true);
				
		this.teiidServer = new EmbeddedServer();
		this.teiidServer.start(ec);		
		this.teiidServer.addTranslator("grid-translator", translator);	

		try {
			addTempVDB();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	private void addTempVDB() throws TranslatorException, VirtualDatabaseException, ConnectorManagerException {
		EmbeddedServer.ConnectionFactoryProvider<Void> gridFactory = new EmbeddedServer.ConnectionFactoryProvider<Void>() {
			@Override
			public Void getConnectionFactory() throws TranslatorException {
				return null;
			}
		};
		this.teiidServer.addConnectionFactoryProvider("grid-source", gridFactory);
			
		ModelMetaData gridModel = new ModelMetaData();
		gridModel.setName("temp");
		gridModel.setSchemaSourceType("ddl");
		gridModel.setSchemaText("CREATE FOREIGN TABLE G1 (e1 string, e2 integer);");
		gridModel.addSourceMapping("grid-connector", "grid-translator", "grid-source");

		// deploy the VDB to the embedded server
		teiidServer.deployVDB("thevdb", gridModel);
	}	
	
	public void addVDB(Iterator<Table> tables, final OgmSessionFactory sf) throws TranslatorException, VirtualDatabaseException, ConnectorManagerException {
		teiidServer.undeployVDB("thevdb");
		
		this.teiidServer.addMetadataRepository("grid-metadata", new GridMetadataRespository(tables, sf));
		
		EmbeddedServer.ConnectionFactoryProvider<OgmSessionFactory> gridFactory = new EmbeddedServer.ConnectionFactoryProvider<OgmSessionFactory>() {
			@Override
			public OgmSessionFactory getConnectionFactory() throws TranslatorException {
				return sf;
			}
		};
		
		this.teiidServer.addConnectionFactoryProvider("grid-source", gridFactory);		
		
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
