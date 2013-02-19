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
package org.hibernate.ogm.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.dialect.GridTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class GridTranslatorInitiator implements BasicServiceInitiator<GridTranslator> {
	private static final Log log = LoggerFactory.make();
	public static GridTranslatorInitiator INSTANCE = new GridTranslatorInitiator();

	@Override
	public Class<GridTranslator> getServiceInitiated() {
		return GridTranslator.class;
	}

	@Override
	public GridTranslator initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		Object value = configurationValues.get(GridTranslator.GRID_TRANSLATOR);
		Class<?> translatorClazz;
		if ( value instanceof String ) {
			try {
				translatorClazz = registry.getService(ClassLoaderService.class).classForName( value.toString() );
			}
			catch (RuntimeException e) {
				throw log.translatorClassCannotBeFound( value.toString() );
			}
			if (!GridTranslator.class.isAssignableFrom(translatorClazz) ) {
				throw log.doesNotImplementGridTranslator( value.toString() );
			}
		}
		else {
			throw log.gridTranslatorPropertyOfUnknownType( value.getClass() );
		}		
		
		try {
			GridTranslator translator = (GridTranslator) translatorClazz.newInstance();
			log.useGridTranslator( translator.getClass().getName() );
			return translator;
		} catch (Exception e) {
			throw log.cannotInstantiateGridTranslator(translatorClazz, e);
		}
	}
}
