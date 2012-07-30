/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Iteration2 {
	// Provided by Hibernate OGM
	public abstract class Option<Type extends Option<Type, Identifier>, Identifier> {
		private Class<Type> type;

		public Option(Class<Type> type) {
			this.type = type;
		}

		public abstract Identifier getOptionIdentifier();

		public Class<Type> getOptionType() {
			return type;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;

			Option option = (Option) o;

			if ( !getOptionIdentifier().equals( option.getOptionIdentifier() ) ) return false;
			if ( !type.equals( option.type ) ) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + getOptionIdentifier().hashCode();
			return result;
		}
	}

	// Provided by Hibernate OGM

	/**
	 * Specialized class used by options that defined only once.
	 * Most options should subclass this class
	 */
	public abstract class UniqueOption<Type extends UniqueOption<Type>>
			extends Option<Type, Object> {
		private Object IDENTITY = new Object();

		public UniqueOption(Class<Type> type) {
			super( type );
		}

		public Object getOptionIdentifier() {
			return IDENTITY;
		}
	}

	// Some examples
	// Simple unique option
	public class Quorum extends UniqueOption<Quorum> {
		public Quorum(int read, int write) {
			super( Quorum.class );
			this.read = read;
			this.write = write;
		}

		public void getRead(int read) {
			this.read = read;
		}

		private int read;

		public void getWrite(int write) {
			this.write = write;
		}

		private int write;
	}

	// multiple options
	public class NamedQuery extends Option<NamedQuery, String> {
		public NamedQuery(String name, String hql) {
			super( NamedQuery.class );
			this.name = name;
			this.hql = hql;
		}

		public String getOptionIdentifier() {
			return name;
		}

		public String getName() {
			return name;
		}

		private String name;

		public String getHql() {
			return hql;
		}

		private String hql;
	}

	public <O extends UniqueOption<O>> O getOption(Class<O> optionType) {
		return null;
	}

	public <I,O extends Option<O,I>> Map<I,O> getOptions(Class<O> optionType) {
		return null;
	}

	public void testGridDialect() {
		//getOption is only for unique options
		Quorum quorum = getOption( Quorum.class );
		//for multiple options, use getOptions
		Map<String,NamedQuery> queries = getOptions( NamedQuery.class );
		//Fails to compile as NamedQuery is not a UniqueOption :)
		// NamedQuery query = getOption( NamedQuery.class );
	}
}
