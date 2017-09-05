/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wildfly.jpa.hibernateogm5;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.Environment;
import org.jboss.as.jpa.hibernate5.HibernatePersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;

/**
 * This adaptor is picked up by JipiJapa to inject specific services needed by Hibernate OGM when booting on WildFly.
 * Among other things, it allows for automatic discovery of entities, wiring of the JTA platform, handling
 * 2nd level cache integration.
 *
 * @author Sanne Grinovero
 */
public class HibernateOGMPersistenceProviderAdaptor implements PersistenceProviderAdaptor, TwoPhaseBootstrapCapable {

	private final HibernatePersistenceProviderAdaptor ormOriginalAdaptor = new HibernatePersistenceProviderAdaptor();

	@Override
	public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
		ormOriginalAdaptor.addProviderProperties( properties, pu );
		// Use "putIfAbsent" to allow overriding the setting:
		properties.putIfAbsent( "hibernate.ogm.enabled", "true" );
		properties.putIfAbsent( Environment.DATASOURCE, "---PlaceHolderDSForOGM---" );
	}

	/* All methods below delegate to the original Hibernate ORM 5 adaptor */

	@Override
	public void injectJtaManager(JtaManager jtaManager) {
		ormOriginalAdaptor.injectJtaManager( jtaManager );
	}

	@Override
	public void injectPlatform(Platform platform) {
		ormOriginalAdaptor.injectPlatform( platform );
	}

	@Override
	public void addProviderDependencies(PersistenceUnitMetadata pu) {
		ormOriginalAdaptor.addProviderDependencies( pu );
	}

	@Override
	public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
		ormOriginalAdaptor.beforeCreateContainerEntityManagerFactory( pu );
	}

	@Override
	public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
		ormOriginalAdaptor.afterCreateContainerEntityManagerFactory( pu );
	}

	@Override
	public ManagementAdaptor getManagementAdaptor() {
		return ormOriginalAdaptor.getManagementAdaptor();
	}

	@Override
	public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
		return ormOriginalAdaptor.doesScopedPersistenceUnitNameIdentifyCacheRegionName( pu );
	}

	@Override
	public void cleanup(PersistenceUnitMetadata pu) {
		ormOriginalAdaptor.cleanup( pu );
	}

	@Override
	public EntityManagerFactoryBuilder getBootstrap(PersistenceUnitInfo info, Map map) {
		return ormOriginalAdaptor.getBootstrap( info, map );
	}

	@Override
	public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {
		ormOriginalAdaptor.markPersistenceUnitAvailable( wrapperBeanManagerLifeCycle );
	}

	@Override
	public Object beanManagerLifeCycle(BeanManager beanManager) {
		return ormOriginalAdaptor.beanManagerLifeCycle( beanManager );
	}
}
