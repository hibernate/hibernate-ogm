/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.infinispan.impl;

import org.infinispan.commons.marshall.AdvancedExternalizer;

/**
 * The ids of our {@link AdvancedExternalizer} implementations used for (de-)serializing key objects from/into
 * Infinispan. The range 1400 - 1499 is <a
 * href="http://infinispan.org/docs/6.0.x/user_guide/user_guide.html#_advanced_externalizers">reserved</a> for OGM.
 *
 * @author Gunnar Morling
 */
class ExternalizerIds {

	static final int ENTITY_KEY = 1400;
	static final int ASSOCIATION_KEY = 1401;
	static final int ROW_KEY = 1402;
	static final int ENTITY_METADATA = 1403;

	private ExternalizerIds() {
	}
}
