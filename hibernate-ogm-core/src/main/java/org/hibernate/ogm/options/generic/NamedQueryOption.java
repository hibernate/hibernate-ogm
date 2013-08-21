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
package org.hibernate.ogm.options.generic;

import org.hibernate.ogm.options.spi.Option;

/**
 * An {@link Option} representing a named query.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class NamedQueryOption extends Option<String, NamedQueryOption> {

	private final String name;
	private final String hql;

	public NamedQueryOption(String name, String hql) {
		this.name = name;
		this.hql = hql;
	}

	@Override
	public String getOptionIdentifier() {
		return name;
	}

	public String getName() {
		return name;
	}

	public String getHql() {
		return hql;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ( ( hql == null ) ? 0 : hql.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals( obj ) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		NamedQueryOption other = (NamedQueryOption) obj;
		if ( hql == null ) {
			if ( other.hql != null ) {
				return false;
			}
		}
		else if ( !hql.equals( other.hql ) ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		return true;
	}
}
