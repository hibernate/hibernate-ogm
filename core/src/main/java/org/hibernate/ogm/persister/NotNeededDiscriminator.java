/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.persister;

import org.hibernate.type.Type;

/**
 * Used when a discriminator is not needed.
 *
 * @author "Davide D'Alto" <davide@hiberante.org>
 */
public class NotNeededDiscriminator implements EntityDiscriminator {

	public static final NotNeededDiscriminator INSTANCE = new NotNeededDiscriminator();

	private NotNeededDiscriminator() {
	}

	@Override
	public String provideClassByValue(Object value) {
		return null;
	}

	@Override
	public String getSqlValue() {
		return null;
	}

	@Override
	public String getColumnName() {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public boolean isForced() {
		return false;
	}

	@Override
	public boolean isNeeded() {
		return false;
	}

}
