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
package org.hibernate.ogm.transaction.impl;

import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.internal.BitronixJtaPlatform;
import org.hibernate.service.jta.platform.internal.BorlandEnterpriseServerJtaPlatform;
import org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.service.jta.platform.internal.JOTMJtaPlatform;
import org.hibernate.service.jta.platform.internal.JOnASJtaPlatform;
import org.hibernate.service.jta.platform.internal.JRun4JtaPlatform;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.service.jta.platform.internal.NoJtaPlatform;
import org.hibernate.service.jta.platform.internal.OC4JJtaPlatform;
import org.hibernate.service.jta.platform.internal.OrionJtaPlatform;
import org.hibernate.service.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.service.jta.platform.internal.SunOneJtaPlatform;
import org.hibernate.service.jta.platform.internal.TransactionManagerLookupBridge;
import org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform;
import org.hibernate.service.jta.platform.internal.WebSphereJtaPlatform;
import org.hibernate.service.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmJtaPlatformInitiator implements BasicServiceInitiator<JtaPlatform> {
	public static final OgmJtaPlatformInitiator INSTANCE = new OgmJtaPlatformInitiator();

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
            CoreMessageLogger.class,
            JtaPlatformInitiator.class.getName()
    );

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if ( ! hasExplicitPlatform( configurationValues ) ) {
			return new JBossStandAloneJtaPlatform();
		}
		return JtaPlatformInitiator.INSTANCE.initiateService(configurationValues, registry);
	}

	private boolean hasExplicitPlatform(Map configVales) {
		Object platform = configVales.get( AvailableSettings.JTA_PLATFORM );
		if ( platform == null ) {
			final String transactionManagerLookupImplName = (String) configVales.get( Environment.TRANSACTION_MANAGER_STRATEGY );
			return transactionManagerLookupImplName != null;
		}
		else {
			return true;
		}
	}
}