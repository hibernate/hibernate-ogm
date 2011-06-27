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
package org.hibernate.ogm.jpa.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ejb.cfg.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.ogm.id.impl.OgmIdentityGenerator;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;

/**
 * Provides a registry of JPA identifier generator types and
 * it's corresponding OGM implementations.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class OgmIdentifierGeneratorStrategyProvider implements IdentifierGeneratorStrategyProvider {
	/**
	 * @return The registry of different JPA identifier generator names
	 *         and their corresponding generator implementations for grid.
	 */
	@Override
	public Map<String, Class<?>> getStrategies() {
		Map<String, Class<?>> strategies = new HashMap<String, Class<?>>();
		strategies.put(
				org.hibernate.id.enhanced.TableGenerator.class.getName(),
				OgmTableGenerator.class
		);
		strategies.put(
				org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName(),
				OgmSequenceGenerator.class
		);
		strategies.put(
				"identity",
				OgmIdentityGenerator.class
		);
		return Collections.unmodifiableMap( strategies );
	}
}
