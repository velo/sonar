/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.util;

import org.junit.Before;
import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class IntegerTypeValidationTest {

  IntegerTypeValidation validation;

  @Before
  public void setUp() throws Exception {
    validation = new IntegerTypeValidation();
  }

  @Test
  public void key() {
    assertThat(validation.key()).isEqualTo("INTEGER");
  }

  @Test
  public void not_fail_on_valid_integer() {
    validation.validate("10", null);
  }

  @Test
  public void fail_on_invalid_integer() {
    try {
      validation.validate("abc", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.l10nParams().toArray()[0]).isEqualTo("abc");
    }
  }

}
