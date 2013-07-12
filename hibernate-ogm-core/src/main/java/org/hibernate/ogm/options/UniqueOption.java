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
package org.hibernate.ogm.options;

import org.hibernate.ogm.mapping.impl.OptionsContainer;

/**
 * Specialized class used by options that defined only once.
 * Most options should subclass this class
 */
public abstract class UniqueOption<Type extends UniqueOption<Type>> extends Option<Object, Type> {
	private static final Object IDENTITY = new Object();

	public UniqueOption() {
	}

	@Override
	public Object getOptionIdentifier() {
		return IDENTITY;
	}

	protected void removeIfSameType(OptionsContainer container) {
		Option<?, ?> existing = null;
		for ( Option<?, ?> option : container ) {
			if ( getOptionType().equals( option.getOptionType() ) ) {
				existing = option;
				break;
			}
		}
		if ( existing != null ) {
			container.remove( existing );
		}
	}

	@Override
	public void apply(OptionsContainer container) {
		removeIfSameType( container );
		container.add( this );
	}
}
