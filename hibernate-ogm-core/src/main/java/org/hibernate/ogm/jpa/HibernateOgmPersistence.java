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
package org.hibernate.ogm.jpa;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.jpa.impl.DelegatorPersistenceUnitInfo;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.jpa.impl.OgmIdentifierGeneratorStrategyProvider;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * JPA PersistenceProvider implementation specific to Hibernate OGM
 * All specific configurations are set transparently for the user.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class HibernateOgmPersistence implements PersistenceProvider {
	private static String IMPLEMENTATION_NAME = HibernateOgmPersistence.class.getName();
	private HibernatePersistence delegate = new HibernatePersistence();
	private static final Log log = LoggerFactory.make();
	private static final Map EMPTY_MAP = new HashMap<Object, Object>( 0 );

	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
		try {
			Map integration = map == null ?
						EMPTY_MAP :
						Collections.unmodifiableMap( map );
			Enumeration<URL> persistenceXml = Thread.currentThread()
					.getContextClassLoader()
					.getResources( "META-INF/persistence.xml" );
			if ( ! persistenceXml.hasMoreElements() ) {
				log.persistenceXmlNotFoundInClassPath( emName != null ? emName : "" );
			}
			while ( persistenceXml.hasMoreElements() ) {
				URL url = persistenceXml.nextElement();
				List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(
						url,
						integration,
						new EJB3DTDEntityResolver(),
						PersistenceUnitTransactionType.RESOURCE_LOCAL
				);
				for ( PersistenceMetadata metadata : metadataFiles ) {
					//if the provider is not set, don't use it as people might want to use Hibernate ORM
					if ( IMPLEMENTATION_NAME.equalsIgnoreCase(
							metadata.getProvider()
					) ) {
						//correct provider
						Map<Object,Object> protectiveCopy = new HashMap<Object,Object>(integration);
						enforceOgmConfig( protectiveCopy );
						protectiveCopy.put( HibernatePersistence.PROVIDER, delegate.getClass().getName() );
						final EntityManagerFactory coreEMF = delegate.createEntityManagerFactory(
								emName, protectiveCopy
						);
						if ( coreEMF != null ) {
							//delegate might return null to refuse the configuration
							//(like when the configuration file is not defining the expected persistent unit)
							return new OgmEntityManagerFactory( coreEMF );
						}
					}
				}
			}
			//not the right provider
			return null;
		}
		catch (PersistenceException pe) {
			throw (PersistenceException) pe;
		}
		catch (Exception e) {
			throw new PersistenceException( "Unable to build EntityManagerFactory", e );
		}
	}

	private void enforceOgmConfig(Map<Object,Object> map) {
		map.put( AvailableSettings.NAMING_STRATEGY, OgmNamingStrategy.class.getName() );
		//we use a placeholder DS to make sure, Hibernate EntityManager (Ejb3Configuration) does not enforce a different connection provider
		map.put( Environment.DATASOURCE, "---PlaceHolderDSForOGM---" );
		map.put( AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER, OgmIdentifierGeneratorStrategyProvider.class.getName());
		map.put( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" ); //needed to guarantee the table id generator mapping
		map.put( OgmConfiguration.OGM_ON, "true" );
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
		final String persistenceProviderClassName = info.getPersistenceProviderClassName();
		if ( persistenceProviderClassName == null || IMPLEMENTATION_NAME.equals( persistenceProviderClassName ) ) {
			Map<Object,Object> protectiveCopy = map != null ? new HashMap<Object,Object>(map) : new HashMap<Object,Object>();
			enforceOgmConfig( protectiveCopy );
			//HEM only builds an EntityManagerFactory when HibernatePersistence.class.getName() is the PersistenceProvider
			//that's why we override it when
			//new DelegatorPersistenceUnitInfo(info)
			final EntityManagerFactory coreEMF = delegate.createContainerEntityManagerFactory(
					new DelegatorPersistenceUnitInfo(
							info
					),
					protectiveCopy
			);
			if ( coreEMF != null ) {
				//delegate might return null to refuse the configuration
				//(like when the configuration file is not defining the expected persistent unit)
				return new OgmEntityManagerFactory( coreEMF );
			}
		}
		//not the right provider
		return null;
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return delegate.getProviderUtil();
	}
}
