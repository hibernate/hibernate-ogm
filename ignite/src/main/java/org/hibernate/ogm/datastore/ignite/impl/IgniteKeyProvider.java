package org.hibernate.ogm.datastore.ignite.impl;

import org.apache.commons.lang.StringUtils;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.hibernate.ogm.datastore.ignite.persistencestrategy.IgniteSerializableEntityKey;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

public class IgniteKeyProvider {

	public static IgniteKeyProvider INSTANCE = new IgniteKeyProvider();
	
	public IgniteSerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new IgniteSerializableEntityKey( key );
	}

	/**
	 * �������������� ����������� ����� �������� � ��������� ����
	 * @param key ���� �������� 
	 * @return ��������� ����
	 */
	public String getEntityKeyString(EntityKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * �������������� ����������� ����� ����� � ��������� ����
	 * @param key ���� �����
	 * @return ��������� ����
	 */
	public String getAssociationKeyString(AssociationKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * �������������� ����������� ����� �������� � ��������� ����
	 * @param key ���� ��������
	 * @return ��������� ����
	 */
	public String getIdSourceKeyString(IdSourceKey key) {
		return getKeyStringByColumnValues(key.getColumnValues());
	}
	
	/**
	 * �������������� ����������� ����� ������ � ��������� ����
	 * @param key ���� ������
	 * @return ��������� ����
	 */
	public String getRowKeyString(RowKey rowKey) {
		return getKeyStringByColumnValues(rowKey.getColumnValues());
	}
	
	private String getKeyStringByColumnValues(Object[] columnValues) {
		return StringUtils.join(columnValues, "-");
	}
	
	/**
	 * ��������� ���� ������� �� ���������� 
	 * @param keyMetadata ����������
	 * @return
	 */
	public String getEntityType(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("������������ ��� �������� " + entity);
			return arr[1];
		}
		else
			return keyMetadata.getTable();
	}
	
	/**
	 * ��������� ����� ���� �� ���������� 
	 * @param keyMetadata ����������
	 * @return
	 */
	public String getEntityCache(EntityKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("EntityKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("������������ ��� �������� " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getTable();
	}
	
	public String getCacheName(String entity) {
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("������������ ��� �������� " + entity);
			return arr[0];
		}
		return entity;
	}
	
	/**
	 * ��������� ����� ���� �� ���������� 
	 * @param keyMetadata ����������
	 * @return
	 */
	public String getAssociationCache(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		String entity = keyMetadata.getTable();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
//			if (arr.length != 2)
//				throw new IgniteHibernateException("������������ ��� �������� " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getTable();
	}
	
	/**
	 * ��������� ���� ������� �� ���������� 
	 * @param keyMetadata ����������
	 * @return
	 */
	public String getAssociationType(AssociationKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
//		String entity = keyMetadata.getTable();
//		if (entity.indexOf(".") >= 0){
//			String[] arr = entity.split("\\.");
////			if (arr.length != 2)
////				throw new IgniteHibernateException("������������ ��� �������� " + entity);
//			return arr[arr.length - 1];
//		}
//		else
			return keyMetadata.getTable();
	}
	
	/**
	 * ��������� ����� ���� �� ���������� 
	 * @param keyMetadata ����������
	 * @return
	 */
	public String getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		if (keyMetadata == null)
			throw new IgniteHibernateException("AssociationKeyMetadata is null");
		String entity = keyMetadata.getName();
		if (entity.indexOf(".") >= 0){
			String[] arr = entity.split("\\.");
			if (arr.length != 2)
				throw new IgniteHibernateException("������������ ��� �������� " + entity);
			return arr[0];
		}
		else
			return keyMetadata.getName();
	}
}
