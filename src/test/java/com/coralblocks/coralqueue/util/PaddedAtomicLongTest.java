/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralqueue.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

public class PaddedAtomicLongTest {

	@Test
	public void testValueSemantics() {
		PaddedAtomicLong value = new PaddedAtomicLong(1);

		Assert.assertEquals(1, value.get());
		value.set(2);
		Assert.assertEquals(2, value.get());
		value.lazySet(3);
		Assert.assertEquals(3, value.get());
	}

	@Test
	public void testValueIsPaddedOnBothSides() {
		Class<?> rightPaddingClass = PaddedAtomicLong.class.getSuperclass();
		Class<?> valueClass = rightPaddingClass.getSuperclass();
		Class<?> leftPaddingClass = valueClass.getSuperclass();

		assertPaddingFields(leftPaddingClass);
		assertPaddingFields(rightPaddingClass);

		Field[] valueFields = valueClass.getDeclaredFields();
		Assert.assertEquals(1, valueFields.length);
		Assert.assertEquals(long.class, valueFields[0].getType());
		Assert.assertTrue(Modifier.isVolatile(valueFields[0].getModifiers()));
	}

	private static void assertPaddingFields(Class<?> paddingClass) {
		Field[] fields = paddingClass.getDeclaredFields();
		Assert.assertEquals(7, fields.length);
		for(Field field : fields) {
			Assert.assertEquals(long.class, field.getType());
			Assert.assertFalse(Modifier.isPublic(field.getModifiers()));
			Assert.assertFalse(Modifier.isVolatile(field.getModifiers()));
		}
	}
}
