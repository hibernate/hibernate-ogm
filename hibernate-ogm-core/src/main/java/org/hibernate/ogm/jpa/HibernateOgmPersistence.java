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

import org.slf4j.Logger;

import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.HibernatePersistence;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
import org.hibernate.ogm.dialect.NoopDialect;
import org.hibernate.ogm.jdbc.NoopConnectionProvider;
import org.hibernate.ogm.jpa.impl.DelegatorPersistenceUnitInfo;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.util.CollectionHelper;

/**
 * JPA PersistenceProvider implementation specific to Hibernate OGM
 * All specific configurations are set transparently for the user.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class HibernateOgmPersistence implements PersistenceProvider {
	private static String IMPLEMENTATION_NAME = HibernateOgmPersistence.class.getName();
	private HibernatePersistence delegate = new HibernatePersistence();
	private static Logger LOG = LoggerFactory.make();

	@Override
	public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
		try {
			Map integration = map == null ?
						CollectionHelper.EMPTY_MAP :
						Collections.unmodifiableMap( map );
			Enumeration<URL> persistenceXml = Thread.currentThread()
					.getContextClassLoader()
					.getResources( "META-INF/persistence.xml" );
			if ( ! persistenceXml.hasMoreElements() ) {
				LOG.warn( "Could not find any META-INF/persistence.xml file in the classpath. " +
						"Unable to build Persistence Unit " + (emName != null ? emName : "") );
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
					if ( metadata.getProvider() == null || IMPLEMENTATION_NAME.equalsIgnoreCase(
							metadata.getProvider()
					) ) {
						//correct provider
						Map<Object,Object> protectiveCopy = new HashMap<Object,Object>(map);
						enforceOgmConfig( protectiveCopy );
						protectiveCopy.put( HibernatePersistence.PROVIDER, delegate.getClass().getName() );
						return delegate.createEntityManagerFactory( emName, protectiveCopy );
					}
				}
			}
			//not the right provider
			return null;
		}
		catch (Exception e) {
			if ( e instanceof PersistenceException ) {
				throw (PersistenceException) e;
			}
			else {
				throw new PersistenceException( "Unable to build EntityManagerFactory", e );
			}
		}
	}

	private void enforceOgmConfig(Map<Object,Object> map) {
		map.put( AvailableSettings.SESSION_FACTORY_OBSERVER, GridMetadataManager.class.getName() );
		map.put( AvailableSettings.NAMING_STRATEGY, OgmNamingStrategy.class.getName() );
		map.put( Environment.CONNECTION_PROVIDER, NoopConnectionProvider.class );
		map.put( Environment.DIALECT, NoopDialect.class );
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
		final String persistenceProviderClassName = info.getPersistenceProviderClassName();
		if ( persistenceProviderClassName == null || IMPLEMENTATION_NAME.equals( persistenceProviderClassName ) ) {
			Map<Object,Object> protectiveCopy = new HashMap<Object,Object>(map);
			enforceOgmConfig( protectiveCopy );
			//HEM only builds an EntityManagerFactory when HibernatePersistence.class.getName() is the PersistenceProvider
			//that's why we override it when
			//new DelegatorPersistenceUnitInfo(info)
			return delegate.createContainerEntityManagerFactory( new DelegatorPersistenceUnitInfo( info ), protectiveCopy );
		}
		else {
			//not the right provider
			return null;
		}
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return delegate.getProviderUtil();
	}
}
