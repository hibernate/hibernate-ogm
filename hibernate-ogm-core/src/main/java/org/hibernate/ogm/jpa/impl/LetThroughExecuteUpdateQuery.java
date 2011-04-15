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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.hibernate.ogm.exception.NotSupportedException;

/**
 * Let through executeUpdate() operations and raise not supported exceptions otherwise
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LetThroughExecuteUpdateQuery implements Query {

		@Override
		public List getResultList() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Object getSingleResult() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public int executeUpdate() {
			return 0;
		}

		@Override
		public Query setMaxResults(int maxResult) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public int getMaxResults() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setFirstResult(int startPosition) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public int getFirstResult() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setHint(String hintName, Object value) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Map<String, Object> getHints() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public <T> Query setParameter(Parameter<T> param, T value) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(String name, Object value) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(String name, Calendar value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(String name, Date value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(int position, Object value) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(int position, Calendar value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setParameter(int position, Date value, TemporalType temporalType) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Set<Parameter<?>> getParameters() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Parameter<?> getParameter(String name) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public <T> Parameter<T> getParameter(String name, Class<T> type) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Parameter<?> getParameter(int position) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public <T> Parameter<T> getParameter(int position, Class<T> type) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public boolean isBound(Parameter<?> param) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public <T> T getParameterValue(Parameter<T> param) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Object getParameterValue(String name) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Object getParameterValue(int position) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setFlushMode(FlushModeType flushMode) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public FlushModeType getFlushMode() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public Query setLockMode(LockModeType lockMode) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public LockModeType getLockMode() {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}

		@Override
		public <T> T unwrap(Class<T> cls) {
			throw new NotSupportedException( "OGM-21", "JP-QL queries are not supported yet" );
		}
	}
