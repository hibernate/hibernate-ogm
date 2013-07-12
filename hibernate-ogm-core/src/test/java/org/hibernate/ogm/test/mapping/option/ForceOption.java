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
package org.hibernate.ogm.test.mapping.option;

import org.hibernate.ogm.options.UniqueOption;

public class ForceOption extends UniqueOption<ForceOption> {

	public static final ForceOption TRUE = new ForceOption( true );
	public static final ForceOption FALSE = new ForceOption( false );

	private final boolean force;

	private ForceOption(boolean force) {
		this.force = force;
	}

	public boolean isForced() {
		return force;
	}

	public static ForceOption valueOf(boolean force) {
		return force ? TRUE : FALSE;
	}

	@Override
	public String toString() {
		return "forced: " + force;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ( force ? 1231 : 1237 );
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
		ForceOption other = (ForceOption) obj;
		if ( force != other.force ) {
			return false;
		}
		return true;
	}

}
