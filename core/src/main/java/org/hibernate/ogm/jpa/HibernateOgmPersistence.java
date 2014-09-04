/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.jpa.impl.DelegatorPersistenceUnitInfo;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.jpa.impl.OgmIdentifierGeneratorStrategyProvider;
import org.hibernate.ogm.query.impl.OgmQueryTranslatorFactory;

/**
 * JPA PersistenceProvider implementation specific to Hibernate OGM
 * All specific configurations are set transparently for the user.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateOgmPersistence implements PersistenceProvider {
	private static String IMPLEMENTATION_NAME = HibernateOgmPersistence.class.getName();

	private final HibernatePersistenceProvider delegate = new HibernatePersistenceProvider();

	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
		try {
			Map<?, ?> integration = map == null ? Collections.emptyMap() : Collections.unmodifiableMap( map );

			List<ParsedPersistenceXmlDescriptor> metadataFiles = PersistenceXmlParser.locatePersistenceUnits(
					integration
			);

			for ( ParsedPersistenceXmlDescriptor metadata : metadataFiles ) {
				//if the provider is not set, don't use it as people might want to use Hibernate ORM
				if ( IMPLEMENTATION_NAME.equalsIgnoreCase(
						metadata.getProviderClassName()
				) ) {
					//correct provider
					Map<Object,Object> protectiveCopy = new HashMap<Object,Object>(integration);
					enforceOgmConfig( protectiveCopy );
					protectiveCopy.put( AvailableSettings.PROVIDER, delegate.getClass().getName() );
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

			//not the right provider
			return null;
		}
		catch (PersistenceException pe) {
			throw pe;
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
		map.put( InternalProperties.OGM_ON, "true" );
		map.put( org.hibernate.cfg.AvailableSettings.QUERY_TRANSLATOR, OgmQueryTranslatorFactory.class.getName() );
		//set the OGM specific mass indexer in case we use Hibernate Search
		map.put( "hibernate.search.massindexer.factoryclass", "org.hibernate.ogm.massindex.impl.OgmMassIndexerFactory" );
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

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		throw new IllegalStateException( "Hibernate OGM does not support schema generation" );
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		throw new IllegalStateException( "Hibernate OGM does not support schema generation" );
	}
}
